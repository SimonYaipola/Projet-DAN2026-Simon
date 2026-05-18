package com.craftboard.server.service;

import com.craftboard.core.dto.PoleRequest;
import com.craftboard.core.dto.PoleResponse;
import com.craftboard.server.entity.Pole;
import com.craftboard.server.repository.PoleRepository;
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
public class PoleService {

    private static final List<String> DEFAULT_POLES = List.of("LeatherWorking", "Forge", "Couture", "Maconnerie");

    private final PoleRepository repository;
    private final EntityLookupService lookup;

    public PoleService(PoleRepository repository, EntityLookupService lookup) {
        this.repository = repository;
        this.lookup = lookup;
    }

    public List<PoleResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public List<PoleResponse> findByCity(Long cityId) {
        ensureDefaultPoles(cityId);
        return repository.findByCityId(cityId).stream().map(this::toResponse).toList();
    }

    public PoleResponse findById(Long id) {
        return toResponse(lookup.pole(id));
    }

    public PoleResponse create(PoleRequest request) {
        Pole pole = new Pole();
        apply(pole, request);
        return toResponse(repository.save(pole));
    }

    public PoleResponse update(Long id, PoleRequest request) {
        Pole pole = lookup.pole(id);
        apply(pole, request);
        return toResponse(repository.save(pole));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pole not found: " + id);
        }
        repository.deleteById(id);
    }

    private void apply(Pole pole, PoleRequest request) {
        pole.setCity(lookup.city(request.cityId()));
        pole.setName(request.name());
    }

    private void ensureDefaultPoles(Long cityId) {
        DEFAULT_POLES.forEach(name -> repository.findByCityIdAndName(cityId, name)
                .orElseGet(() -> {
                    Pole pole = new Pole();
                    pole.setCity(lookup.city(cityId));
                    pole.setName(name);
                    return repository.save(pole);
                }));
    }

    private PoleResponse toResponse(Pole pole) {
        return new PoleResponse(pole.getId(), pole.getCity().getId(), pole.getName());
    }
}
