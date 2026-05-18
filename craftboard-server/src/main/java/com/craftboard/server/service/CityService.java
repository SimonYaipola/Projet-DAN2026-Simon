package com.craftboard.server.service;

import com.craftboard.core.dto.CityRequest;
import com.craftboard.core.dto.CityResetResponse;
import com.craftboard.core.dto.CityResponse;
import com.craftboard.server.entity.City;
import com.craftboard.server.repository.ActivityLogRepository;
import com.craftboard.server.repository.AppUserRepository;
import com.craftboard.server.repository.CraftOrderRepository;
import com.craftboard.server.repository.EquipmentRepository;
import com.craftboard.server.repository.PoleRepository;
import com.craftboard.server.repository.QuestRepository;
import com.craftboard.server.repository.QuestSignupRepository;
import com.craftboard.server.repository.CityRepository;
import org.springframework.beans.factory.annotation.Value;
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
public class CityService {

    private final CityRepository repository;
    private final EntityLookupService lookup;
    private final String appAdminPassword;
    private final ActivityLogRepository activityLogRepository;
    private final CraftOrderRepository craftOrderRepository;
    private final EquipmentRepository equipmentRepository;
    private final QuestRepository questRepository;
    private final QuestSignupRepository questSignupRepository;
    private final PoleRepository poleRepository;
    private final AppUserRepository appUserRepository;

    public CityService(
            CityRepository repository,
            EntityLookupService lookup,
            @Value("${craftboard.app-admin-password}") String appAdminPassword,
            ActivityLogRepository activityLogRepository,
            CraftOrderRepository craftOrderRepository,
            EquipmentRepository equipmentRepository,
            QuestRepository questRepository,
            QuestSignupRepository questSignupRepository,
            PoleRepository poleRepository,
            AppUserRepository appUserRepository) {
        this.repository = repository;
        this.lookup = lookup;
        this.appAdminPassword = appAdminPassword;
        this.activityLogRepository = activityLogRepository;
        this.craftOrderRepository = craftOrderRepository;
        this.equipmentRepository = equipmentRepository;
        this.questRepository = questRepository;
        this.questSignupRepository = questSignupRepository;
        this.poleRepository = poleRepository;
        this.appUserRepository = appUserRepository;
    }

    public List<CityResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public List<CityResponse> findImportedFromBitjita() {
        return repository.findByApiUrlStartingWith("https://bitjita.com/claims/")
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CityResponse findById(Long id) {
        return toResponse(lookup.city(id));
    }

    public CityResponse create(CityRequest request) {
        City city = new City();
        apply(city, request);
        return toResponse(repository.save(city));
    }

    public CityResponse update(Long id, CityRequest request) {
        City city = lookup.city(id);
        apply(city, request);
        return toResponse(repository.save(city));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found: " + id);
        }
        repository.deleteById(id);
    }

    public CityResetResponse resetImportedCity(Long id, String adminPassword) {
        if (!appAdminPassword.equals(adminPassword)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid application admin password");
        }
        City city = lookup.city(id);
        if (city.getApiUrl() == null || !city.getApiUrl().startsWith("https://bitjita.com/claims/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only imported BitJita cities can be reset");
        }
        String cityName = city.getName();
        activityLogRepository.findByCityIdOrderByCreatedAtDesc(id).forEach(activityLogRepository::delete);
        craftOrderRepository.findByCityId(id).forEach(craftOrderRepository::delete);
        equipmentRepository.findByCityId(id).forEach(equipmentRepository::delete);
        questSignupRepository.findByQuestCityId(id).forEach(questSignupRepository::delete);
        questRepository.findByCityId(id).forEach(questRepository::delete);
        poleRepository.findByCityId(id).forEach(poleRepository::delete);
        appUserRepository.findByCityId(id).forEach(appUserRepository::delete);
        repository.delete(city);
        return new CityResetResponse(id, cityName, "City reset completed");
    }

    private void apply(City city, CityRequest request) {
        city.setName(request.name());
        city.setApiUrl(request.apiUrl());
        city.setCode(request.code());
    }

    private CityResponse toResponse(City city) {
        return new CityResponse(
                city.getId(),
                city.getName(),
                city.getApiUrl(),
                city.getCode(),
                city.getCreatedAt()
        );
    }
}
