package com.craftboard.server.service;

import com.craftboard.core.dto.EquipmentRequest;
import com.craftboard.core.dto.EquipmentResponse;
import com.craftboard.core.enums.EquipmentCategory;
import com.craftboard.server.entity.Equipment;
import com.craftboard.server.repository.EquipmentRepository;
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
public class EquipmentService {

    private final EquipmentRepository repository;
    private final EntityLookupService lookup;

    public EquipmentService(EquipmentRepository repository, EntityLookupService lookup) {
        this.repository = repository;
        this.lookup = lookup;
    }

    public List<EquipmentResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public List<EquipmentResponse> findByCity(Long cityId) {
        return repository.findByCityId(cityId).stream().map(this::toResponse).toList();
    }

    public EquipmentResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    public EquipmentResponse create(EquipmentRequest request) {
        Equipment equipment = new Equipment();
        apply(equipment, request);
        return toResponse(repository.save(equipment));
    }

    public EquipmentResponse update(Long id, EquipmentRequest request) {
        Equipment equipment = findEntity(id);
        apply(equipment, request);
        return toResponse(repository.save(equipment));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found: " + id);
        }
        repository.deleteById(id);
    }

    private Equipment findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipment not found: " + id));
    }

    private void apply(Equipment equipment, EquipmentRequest request) {
        equipment.setCity(lookup.city(request.cityId()));
        equipment.setOwner(lookup.user(request.ownerId()));
        equipment.setPole(lookup.optionalPole(request.poleId()));
        equipment.setName(request.name());
        equipment.setCategory(request.category() == null ? EquipmentCategory.OTHER : request.category());
        equipment.setTier(request.tier());
        equipment.setRarity(request.rarity());
        equipment.setConditionValue(request.conditionValue() == null ? 0 : request.conditionValue());
    }

    private EquipmentResponse toResponse(Equipment equipment) {
        return new EquipmentResponse(
                equipment.getId(),
                equipment.getCity().getId(),
                equipment.getOwner().getId(),
                equipment.getPole() == null ? null : equipment.getPole().getId(),
                equipment.getName(),
                equipment.getCategory(),
                equipment.getTier(),
                equipment.getRarity(),
                equipment.getBitjitaItemId(),
                equipment.getSlot(),
                equipment.getIconAssetName(),
                equipment.getImageUrl(),
                equipment.getRarityStr(),
                equipment.getConditionValue(),
                equipment.getCreatedAt()
        );
    }
}
