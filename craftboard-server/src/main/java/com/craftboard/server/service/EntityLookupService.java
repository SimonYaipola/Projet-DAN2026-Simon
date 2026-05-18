package com.craftboard.server.service;

import com.craftboard.server.entity.AppUser;
import com.craftboard.server.entity.City;
import com.craftboard.server.entity.Pole;
import com.craftboard.server.repository.AppUserRepository;
import com.craftboard.server.repository.CityRepository;
import com.craftboard.server.repository.PoleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Regroupe les recherches d'entites et les erreurs si elles sont absentes.
 */
@Service
public class EntityLookupService {

    private final CityRepository cityRepository;
    private final AppUserRepository appUserRepository;
    private final PoleRepository poleRepository;

    public EntityLookupService(
            CityRepository cityRepository,
            AppUserRepository appUserRepository,
            PoleRepository poleRepository) {
        this.cityRepository = cityRepository;
        this.appUserRepository = appUserRepository;
        this.poleRepository = poleRepository;
    }

    public City city(Long id) {
        return cityRepository.findById(id)
                .orElseThrow(() -> notFound("City not found: " + id));
    }

    public AppUser user(Long id) {
        return appUserRepository.findById(id)
                .orElseThrow(() -> notFound("User not found: " + id));
    }

    public Pole pole(Long id) {
        return poleRepository.findById(id)
                .orElseThrow(() -> notFound("Pole not found: " + id));
    }

    public Pole optionalPole(Long id) {
        return id == null ? null : pole(id);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
