package com.craftboard.server.service;

import com.craftboard.core.dto.ActivityLogRequest;
import com.craftboard.core.dto.ActivityLogResponse;
import com.craftboard.server.entity.AppUser;
import com.craftboard.server.entity.ActivityLog;
import com.craftboard.server.repository.ActivityLogRepository;
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
public class ActivityLogService {

    private final ActivityLogRepository repository;
    private final EntityLookupService lookup;

    public ActivityLogService(ActivityLogRepository repository, EntityLookupService lookup) {
        this.repository = repository;
        this.lookup = lookup;
    }

    public List<ActivityLogResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    public List<ActivityLogResponse> findByCity(Long cityId) {
        return repository.findByCityIdOrderByCreatedAtDesc(cityId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ActivityLogResponse findById(Long id) {
        return toResponse(findEntity(id));
    }

    public ActivityLogResponse create(ActivityLogRequest request) {
        ActivityLog log = new ActivityLog();
        log.setCity(lookup.city(request.cityId()));
        log.setUser(lookup.user(request.userId()));
        log.setActionType(request.actionType());
        log.setDescription(request.description());
        return toResponse(repository.save(log));
    }

    public void record(AppUser user, String actionType, String description) {
        if (user == null || user.getCity() == null) {
            return;
        }
        ActivityLog log = new ActivityLog();
        log.setCity(user.getCity());
        log.setUser(user);
        log.setActionType(actionType);
        log.setDescription(description);
        repository.save(log);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity log not found: " + id);
        }
        repository.deleteById(id);
    }

    private ActivityLog findEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity log not found: " + id));
    }

    private ActivityLogResponse toResponse(ActivityLog log) {
        return new ActivityLogResponse(
                log.getId(),
                log.getCity().getId(),
                log.getUser().getId(),
                log.getUser().getUsername(),
                log.getActionType(),
                log.getDescription(),
                log.getCreatedAt()
        );
    }
}
