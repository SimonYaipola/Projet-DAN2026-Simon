package com.craftboard.server.service;

import com.craftboard.core.dto.EquippedItemResponse;
import com.craftboard.core.dto.PlayerEquipmentResponse;
import com.craftboard.core.enums.EquipmentCategory;
import com.craftboard.server.entity.AppUser;
import com.craftboard.server.repository.AppUserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Convertit les donnees Bitjita en equipements utilisables par CraftBoard.
 */
@Service
@Transactional
public class BitjitaEquipmentService {

    private final EntityLookupService lookup;
    private final BitjitaClientService bitjitaClientService;
    private final AppUserRepository appUserRepository;
    private static final List<SlotDefinition> TOOL_SLOTS = List.of(
            new SlotDefinition("axe", "Hache", item -> toolTextMatches(item, " axe", "woodcutter tool")),
            new SlotDefinition("pickaxe", "Pioche", item -> toolTextMatches(item, "pickaxe", "miner tool")),
            new SlotDefinition("saw", "Scie", item -> toolTextMatches(item, "saw", "carpenter tool")),
            new SlotDefinition("hammer", "Marteau", item -> toolTextMatches(item, "hammer", "blacksmith tool")),
            new SlotDefinition("chisel", "Ciseau", item -> toolTextMatches(item, "chisel", "mason tool")),
            new SlotDefinition("hoe", "Houe", item -> toolTextMatches(item, "hoe", "farmer tool")),
            new SlotDefinition("machete", "Machette", item -> toolTextMatches(item, "machete", "forager tool")),
            new SlotDefinition("fishing_rod", "Canne a peche", item -> toolTextMatches(item, " rod", "fisher tool")),
            new SlotDefinition("cooking_pot", "Marmite", item -> toolTextMatches(item, " pot", "cook tool")),
            new SlotDefinition("quill", "Plume", item -> toolTextMatches(item, "quill", "scholar tool")),
            new SlotDefinition("mallet", "Maillet", item -> toolTextMatches(item, "mallet")),
            new SlotDefinition("tuner", "Tuner hexite", item -> toolTextMatches(item, "tuner", "hexite"))
    );

    public BitjitaEquipmentService(
            EntityLookupService lookup,
            BitjitaClientService bitjitaClientService,
            AppUserRepository appUserRepository) {
        this.lookup = lookup;
        this.bitjitaClientService = bitjitaClientService;
        this.appUserRepository = appUserRepository;
    }

    public PlayerEquipmentResponse findEquippedToolsAndArmor(Long userId) {
        AppUser user = lookup.user(userId);
        String playerEntityId = resolvePlayerEntityId(user);
        if (playerEntityId == null || playerEntityId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Ce membre n'a pas encore de playerEntityId BitJita. Reimporte la ville pour synchroniser les membres."
            );
        }

        List<EquippedItemResponse> items = new ArrayList<>();
        addToolbeltTools(items, playerEntityId);

        Map<String, EquippedItemResponse> uniqueItems = new LinkedHashMap<>();
        for (EquippedItemResponse item : items) {
            String key = item.source() + "|" + item.slot() + "|" + item.itemId();
            uniqueItems.putIfAbsent(key, item);
        }

        return new PlayerEquipmentResponse(
                user.getId(),
                user.getUsername(),
                playerEntityId,
                new ArrayList<>(uniqueItems.values())
        );
    }

    private String resolvePlayerEntityId(AppUser user) {
        if (user.getPlayerEntityId() != null && !user.getPlayerEntityId().isBlank()) {
            return user.getPlayerEntityId();
        }
        if (user.getCity() == null || user.getCity().getCode() == null || user.getCity().getCode().isBlank()) {
            return null;
        }

        JsonNode memberPayload = bitjitaClientService.claimMembers(user.getCity().getCode());
        for (JsonNode memberNode : memberPayload.path("members")) {
            String username = firstText(memberNode, "userName", "username", "displayName");
            String playerEntityId = firstText(memberNode, "playerEntityId", "entityId");
            if (username != null && username.equalsIgnoreCase(user.getUsername()) && playerEntityId != null) {
                user.setPlayerEntityId(playerEntityId);
                appUserRepository.save(user);
                return playerEntityId;
            }
        }
        return null;
    }

    private void addToolbeltTools(List<EquippedItemResponse> items, String playerEntityId) {
        JsonNode payload = bitjitaClientService.playerInventories(playerEntityId);
        Map<Long, JsonNode> itemIndex = new LinkedHashMap<>();
        Map<String, JsonNode> toolsBySlot = new LinkedHashMap<>();
        payload.path("items").fields().forEachRemaining(entry -> {
            try {
                itemIndex.put(Long.parseLong(entry.getKey()), entry.getValue());
            } catch (NumberFormatException ignored) {
                // BitJita item keys are numeric in practice, but keep the parser tolerant.
            }
        });

        payload.path("inventories").forEach(inventoryNode -> {
            String inventoryName = text(inventoryNode, "inventoryName", "");
            boolean toolbelt = inventoryName.toLowerCase(Locale.ROOT).replace(" ", "").contains("toolbelt");
            inventoryNode.path("pockets").forEach(pocketNode -> {
                JsonNode contents = pocketNode.path("contents");
                Long itemId = nullableLong(contents, "itemId", "item_id");
                if (itemId == null) {
                    return;
                }
                JsonNode itemNode = itemIndex.get(itemId);
                if (itemNode == null || itemNode.isMissingNode() || itemNode.isNull()) {
                    return;
                }
                if (!toolbelt && !isTool(itemNode)) {
                    return;
                }
                addToolIfMatched(toolsBySlot, itemNode);
            });
        });
        for (SlotDefinition slot : TOOL_SLOTS) {
            JsonNode itemNode = toolsBySlot.get(slot.key());
            if (itemNode == null || itemNode.isMissingNode() || itemNode.isNull()) {
                items.add(emptyItem("Tool belt", slot.label(), EquipmentCategory.TOOL));
            } else {
                items.add(toItem("Tool belt", slot.label(), itemNode, EquipmentCategory.TOOL));
            }
        }
    }

    private void addToolIfMatched(Map<String, JsonNode> toolsBySlot, JsonNode itemNode) {
        for (SlotDefinition slot : TOOL_SLOTS) {
            if (slot.matches(itemNode)) {
                toolsBySlot.putIfAbsent(slot.key(), itemNode);
                return;
            }
        }
    }

    private EquippedItemResponse toItem(String source, String slot, JsonNode itemNode, EquipmentCategory category) {
        Integer tier = nullableInt(itemNode, "tier", "itemTier");
        return new EquippedItemResponse(
                source,
                displaySlot(slot),
                nullableLong(itemNode, "id", "itemId"),
                firstText(itemNode, "name", "itemName"),
                category,
                tier,
                firstText(itemNode, "rarityString", "rarityStr", "itemRarityStr"),
                firstText(itemNode, "tag", "tags"),
                text(itemNode, "iconAssetName", null),
                null
        );
    }

    private EquippedItemResponse emptyItem(String source, String slot, EquipmentCategory category) {
        return new EquippedItemResponse(
                source,
                slot,
                null,
                "Aucune",
                category,
                null,
                null,
                null,
                null,
                null
        );
    }

    private boolean isTool(JsonNode itemNode) {
        if (!itemNode.path("toolType").isMissingNode() || !itemNode.path("toolPower").isMissingNode()) {
            return true;
        }
        String tag = firstText(itemNode, "tag", "tags");
        return tag != null && tag.toLowerCase(Locale.ROOT).contains("tool");
    }

    private static boolean toolTextMatches(JsonNode itemNode, String... values) {
        String combined = (" " + staticText(itemNode, "name", "") + " " + staticFirstText(itemNode, "tag", "tags") + " ")
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ');
        for (String value : values) {
            if (combined.contains(value.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String displaySlot(String slot) {
        if (slot == null || slot.isBlank()) {
            return "-";
        }
        return slot.replace("_clothing", "")
                .replace("_", " ");
    }

    private String text(JsonNode node, String field, String fallback) {
        return staticText(node, field, fallback);
    }

    private static String staticText(JsonNode node, String field, String fallback) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? fallback : value.asText();
    }

    private String firstText(JsonNode node, String... fields) {
        return staticFirstText(node, fields);
    }

    private static String staticFirstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = staticText(node, field, null);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Long nullableLong(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (!value.isMissingNode() && !value.isNull()) {
                return value.asLong();
            }
        }
        return null;
    }

    private Integer nullableInt(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (!value.isMissingNode() && !value.isNull()) {
                return value.asInt();
            }
        }
        return null;
    }

    private record SlotDefinition(String key, String label, Predicate<JsonNode> matcher) {

        private boolean matches(JsonNode itemNode) {
            return matcher.test(itemNode);
        }
    }
}
