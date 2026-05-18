package com.craftboard.server.service;

import com.craftboard.core.dto.AdminUserUpdateRequest;
import com.craftboard.core.dto.UserCreateRequest;
import com.craftboard.core.dto.UserResponse;
import com.craftboard.core.dto.UserUpdateRequest;
import com.craftboard.core.enums.UserRole;
import com.craftboard.server.entity.AppUser;
import com.craftboard.server.repository.AppUserRepository;
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
public class UserService {

    private final AppUserRepository repository;
    private final EntityLookupService lookup;
    private final PasswordService passwordService;
    private final ActivityLogService activityLogService;

    public UserService(
            AppUserRepository repository,
            EntityLookupService lookup,
            PasswordService passwordService,
            ActivityLogService activityLogService) {
        this.repository = repository;
        this.lookup = lookup;
        this.passwordService = passwordService;
        this.activityLogService = activityLogService;
    }

    public List<UserResponse> findAll() {
        return repository.findAll().stream()
                .filter(this::isVisibleCityMember)
                .map(this::toResponse)
                .toList();
    }

    public List<UserResponse> findByCity(Long cityId) {
        return repository.findByCityId(cityId).stream()
                .filter(this::isVisibleCityMember)
                .map(this::toResponse)
                .toList();
    }

    public UserResponse findById(Long id) {
        return toResponse(lookup.user(id));
    }

    public UserResponse create(UserCreateRequest request) {
        AppUser user = new AppUser();
        user.setPasswordHash(passwordService.hash(request.password()));
        user.setPasswordConfigured(true);
        apply(user, request.cityId(), request.username(), request.role(), request.displayName());
        return toResponse(repository.save(user));
    }

    public UserResponse update(Long id, UserUpdateRequest request) {
        AppUser user = lookup.user(id);
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordService.hash(request.password()));
            user.setPasswordConfigured(true);
        }
        apply(user, request.cityId(), request.username(), request.role(), request.displayName());
        AppUser saved = repository.save(user);
        activityLogService.record(saved, "PROFILE_UPDATED", saved.getUsername() + " a modifie son profil");
        return toResponse(saved);
    }

    public UserResponse adminUpdate(Long id, AdminUserUpdateRequest request) {
        AppUser user = lookup.user(id);
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.setDisplayName(request.displayName());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordService.hash(request.password()));
            user.setPasswordConfigured(true);
        }
        AppUser saved = repository.save(user);
        activityLogService.record(saved, "USER_ADMIN_UPDATED", saved.getUsername() + " a ete modifie par un admin");
        return toResponse(saved);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found: " + id);
        }
        repository.deleteById(id);
    }

    private void apply(AppUser user, Long cityId, String username, UserRole role, String displayName) {
        user.setCity(lookup.city(cityId));
        user.setUsername(username);
        user.setRole(role == null ? UserRole.CITIZEN : role);
        user.setDisplayName(displayName);
    }

    private boolean isVisibleCityMember(AppUser user) {
        return user.getPlayerEntityId() != null && !user.getPlayerEntityId().isBlank();
    }

    private UserResponse toResponse(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getCity().getId(),
                user.getUsername(),
                user.getRole(),
                user.getDisplayName(),
                user.getPlayerEntityId(),
                user.getPasswordConfigured(),
                user.getCreatedAt()
        );
    }
}
