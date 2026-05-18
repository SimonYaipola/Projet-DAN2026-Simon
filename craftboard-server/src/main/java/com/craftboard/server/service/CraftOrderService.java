package com.craftboard.server.service;

import com.craftboard.core.dto.CraftOrderRequest;
import com.craftboard.core.dto.CraftOrderResponse;
import com.craftboard.core.dto.OrderActionRequest;
import com.craftboard.core.dto.OrderItemRequest;
import com.craftboard.core.dto.OrderItemResponse;
import com.craftboard.core.enums.OrderStatus;
import com.craftboard.core.enums.UserRole;
import com.craftboard.server.entity.AppUser;
import com.craftboard.server.entity.CraftOrder;
import com.craftboard.server.entity.OrderItem;
import com.craftboard.server.repository.CraftOrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Service metier utilise par les controleurs du serveur.
 */
@Service
@Transactional
public class CraftOrderService {

    private final CraftOrderRepository repository;
    private final EntityLookupService lookup;
    private final ActivityLogService activityLogService;

    public CraftOrderService(
            CraftOrderRepository repository,
            EntityLookupService lookup,
            ActivityLogService activityLogService) {
        this.repository = repository;
        this.lookup = lookup;
        this.activityLogService = activityLogService;
    }

    public List<CraftOrderResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public List<CraftOrderResponse> findByCity(Long cityId) {
        return repository.findByCityId(cityId).stream().map(this::toResponse).toList();
    }

    public CraftOrderResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    public CraftOrderResponse create(CraftOrderRequest request) {
        CraftOrder order = new CraftOrder();
        apply(order, request);
        CraftOrder saved = repository.save(order);
        activityLogService.record(
                saved.getCreatedBy(),
                "ORDER_CREATED",
                "Commande #" + saved.getId() + " creee pour " + saved.getPole().getName()
        );
        return toResponse(saved);
    }

    public CraftOrderResponse update(Long id, CraftOrderRequest request) {
        CraftOrder order = findEntity(id);
        apply(order, request);
        return toResponse(repository.save(order));
    }

    public CraftOrderResponse assignToSelf(Long id, OrderActionRequest request) {
        CraftOrder order = findEntity(id);
        AppUser user = artisanOrAdmin(request.userId());
        ensureSameCity(order, user);
        order.setAssignedTo(user);
        order.setStatus(OrderStatus.IN_PROGRESS);
        CraftOrder saved = repository.save(order);
        activityLogService.record(user, "ORDER_ASSIGNED", "Commande #" + saved.getId() + " prise en charge");
        return toResponse(saved);
    }

    public CraftOrderResponse complete(Long id, OrderActionRequest request) {
        CraftOrder order = findEntity(id);
        AppUser user = artisanOrAdmin(request.userId());
        ensureSameCity(order, user);
        order.setAssignedTo(user);
        order.setStatus(OrderStatus.COMPLETED);
        if (request.notes() != null && !request.notes().isBlank()) {
            order.setNotes(request.notes());
        }
        CraftOrder saved = repository.save(order);
        activityLogService.record(user, "ORDER_COMPLETED", "Commande #" + saved.getId() + " completee");
        return toResponse(saved);
    }

    public CraftOrderResponse cancel(Long id, OrderActionRequest request) {
        CraftOrder order = findEntity(id);
        AppUser user = artisanOrAdmin(request.userId());
        ensureSameCity(order, user);
        order.setStatus(OrderStatus.CANCELLED);
        if (request.notes() != null && !request.notes().isBlank()) {
            order.setNotes(request.notes());
        }
        CraftOrder saved = repository.save(order);
        activityLogService.record(user, "ORDER_CANCELLED", "Commande #" + saved.getId() + " annulee");
        return toResponse(saved);
    }

    public CraftOrderResponse reopen(Long id, OrderActionRequest request) {
        CraftOrder order = findEntity(id);
        AppUser user = artisanOrAdmin(request.userId());
        ensureSameCity(order, user);
        order.setAssignedTo(user);
        order.setStatus(OrderStatus.IN_PROGRESS);
        if (request.notes() != null && !request.notes().isBlank()) {
            order.setNotes(request.notes());
        }
        CraftOrder saved = repository.save(order);
        activityLogService.record(user, "ORDER_REOPENED", "Commande #" + saved.getId() + " remise en cours");
        return toResponse(saved);
    }

    public CraftOrderResponse updateNotes(Long id, OrderActionRequest request) {
        CraftOrder order = findEntity(id);
        AppUser user = artisanOrAdmin(request.userId());
        ensureSameCity(order, user);
        order.setNotes(request.notes());
        CraftOrder saved = repository.save(order);
        activityLogService.record(user, "ORDER_NOTES_UPDATED", "Notes modifiees pour la commande #" + saved.getId());
        return toResponse(saved);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id);
        }
        repository.deleteById(id);
    }

    private CraftOrder findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id));
    }

    private void apply(CraftOrder order, CraftOrderRequest request) {
        // Une commande doit toujours etre rattachee a une ville, un createur et un pole.
        if (request.cityId() == null || request.createdBy() == null || request.poleId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cityId, createdBy and poleId are required");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one order item is required");
        }
        order.setCity(lookup.city(request.cityId()));
        order.setCreatedBy(lookup.user(request.createdBy()));
        order.setPole(lookup.pole(request.poleId()));
        order.setStatus(request.status() == null ? OrderStatus.PENDING : request.status());
        request.items().forEach(item -> validateItemForPole(order.getPole().getName(), item));
        order.setItems(request.items() == null
                ? List.of()
                : request.items().stream().map(this::toEntity).toList());
    }

    private OrderItem toEntity(OrderItemRequest request) {
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be greater than 0");
        }
        validateRarityForTier(request.tier(), request.rarity());
        OrderItem item = new OrderItem();
        item.setCategory(request.category());
        item.setTier(request.tier());
        item.setRarity(request.rarity());
        item.setQuantity(request.quantity());
        item.setToolType(request.toolType());
        item.setEquipmentType(request.equipmentType());
        item.setArmorMaterial(request.armorMaterial());
        return item;
    }

    private void validateItemForPole(String poleName, OrderItemRequest item) {
        // Les champs requis changent selon le pole de craft choisi par l'utilisateur.
        validateRarityForTier(item.tier(), item.rarity());
        String category = item.category();
        if ("LeatherWorking".equals(poleName)) {
            requireArmor(item, "Cuir", poleName);
            return;
        }
        if ("Couture".equals(poleName)) {
            requireArmor(item, "Tissu", poleName);
            return;
        }
        if ("Maconnerie".equals(poleName)) {
            if (!"ARMOR".equals(category) || item.equipmentType() == null || !"Anneau".equals(item.equipmentType())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Maconnerie can only craft rings");
            }
            return;
        }
        if ("Forge".equals(poleName)) {
            if ("TOOL".equals(category)) {
                String toolType = item.toolType();
                if ("Marteau".equals(toolType) || "Marmite".equals(toolType)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, toolType + " is not craftable");
                }
            } else if ("ARMOR".equals(category) && !"Metal".equals(item.armorMaterial())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Forge armor must be metal");
            }
        }
    }

    private void requireArmor(OrderItemRequest item, String material, String poleName) {
        if (!"ARMOR".equals(item.category())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, poleName + " can only craft armor");
        }
        if (!material.equals(item.armorMaterial())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, poleName + " armor material must be " + material);
        }
    }

    private AppUser artisanOrAdmin(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        AppUser user = lookup.user(userId);
        if (user.getRole() != UserRole.ARTISAN && user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only artisans and admins can update orders");
        }
        return user;
    }

    private void ensureSameCity(CraftOrder order, AppUser user) {
        if (!order.getCity().getId().equals(user.getCity().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User and order must be in the same city");
        }
    }

    private void validateRarityForTier(String tier, String rarity) {
        int tierNumber = parseTier(tier);
        int rarityRank = rarityRank(rarity);
        int maxRank = Math.min(4, Math.max(0, tierNumber - 1));
        if (rarityRank > maxRank) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Rarity " + rarity + " is not available for " + tier
            );
        }
    }

    private int parseTier(String tier) {
        if (tier == null || tier.isBlank()) {
            return 1;
        }
        String normalized = tier.trim().toUpperCase().replace("T", "");
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private int rarityRank(String rarity) {
        if (rarity == null) {
            return 0;
        }
        return switch (rarity) {
            case "Uncommon" -> 1;
            case "Rare" -> 2;
            case "Epic" -> 3;
            case "Legendary" -> 4;
            default -> 0;
        };
    }

    private CraftOrderResponse toResponse(CraftOrder order) {
        return new CraftOrderResponse(
                order.getId(),
                order.getCity().getId(),
                order.getCreatedBy().getId(),
                order.getCreatedBy().getUsername(),
                order.getAssignedTo() == null ? null : order.getAssignedTo().getId(),
                order.getAssignedTo() == null ? null : order.getAssignedTo().getUsername(),
                order.getPole().getId(),
                order.getPole().getName(),
                order.getStatus(),
                order.getNotes(),
                order.getCreatedAt(),
                order.getItems().stream().map(this::toItemResponse).toList()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getCategory(),
                item.getTier(),
                item.getRarity(),
                item.getQuantity(),
                item.getToolType(),
                item.getEquipmentType(),
                item.getArmorMaterial()
        );
    }
}
