package com.craftboard.server.service;

import com.craftboard.core.dto.AccountActivationRequest;
import com.craftboard.core.dto.AuthLoginRequest;
import com.craftboard.core.dto.AuthLoginResponse;
import com.craftboard.core.dto.CityResponse;
import com.craftboard.core.dto.SetupAdminRequest;
import com.craftboard.core.dto.UserResponse;
import com.craftboard.core.enums.UserRole;
import com.craftboard.server.entity.AppUser;
import com.craftboard.server.entity.City;
import com.craftboard.server.repository.AppUserRepository;
import com.craftboard.server.repository.CityRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service metier utilise par les controleurs du serveur.
 */
@Service
@Transactional
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final CityRepository cityRepository;
    private final EntityLookupService lookup;
    private final PasswordService passwordService;
    private final ActivityLogService activityLogService;

    public AuthService(
            AppUserRepository appUserRepository,
            CityRepository cityRepository,
            EntityLookupService lookup,
            PasswordService passwordService,
            ActivityLogService activityLogService) {
        this.appUserRepository = appUserRepository;
        this.cityRepository = cityRepository;
        this.lookup = lookup;
        this.passwordService = passwordService;
        this.activityLogService = activityLogService;
    }

    public AuthLoginResponse login(AuthLoginRequest request) {
        AppUser user = appUserRepository.findByCityIdAndUsername(request.cityId(), request.username())
                .orElseThrow(() -> unauthorized());
        if (!Boolean.TRUE.equals(user.getPasswordConfigured())) {
            throw unauthorized();
        }
        if (!passwordService.matches(request.password(), user.getPasswordHash())) {
            throw unauthorized();
        }
        return new AuthLoginResponse(toUserResponse(user), toCityResponse(user.getCity()));
    }

    public AuthLoginResponse createInitialAdmin(Long cityId, SetupAdminRequest request) {
        City city = lookup.city(cityId);
        boolean adminExists = appUserRepository.findByCityId(cityId).stream()
                .anyMatch(user -> user.getRole() == UserRole.ADMIN && Boolean.TRUE.equals(user.getPasswordConfigured()));
        if (adminExists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin already exists for this city");
        }

        AppUser user = appUserRepository.findByCityIdAndUsername(cityId, request.username())
                .orElseGet(AppUser::new);
        user.setCity(city);
        user.setUsername(request.username());
        user.setDisplayName(request.displayName() == null || request.displayName().isBlank()
                ? request.username()
                : request.displayName());
        user.setRole(UserRole.ADMIN);
        user.setPasswordHash(passwordService.hash(request.password()));
        user.setPasswordConfigured(true);
        AppUser saved = appUserRepository.save(user);
        activityLogService.record(saved, "ADMIN_CREATED", "Admin initial cree pour la ville " + city.getName());
        return new AuthLoginResponse(toUserResponse(saved), toCityResponse(city));
    }

    public AuthLoginResponse activateAccount(AccountActivationRequest request) {
        // Le premier acces peut activer un joueur deja importe ou creer un compte local.
        AppUser user = appUserRepository.findByCityIdAndUsername(request.cityId(), request.username())
                .orElseGet(() -> appUserRepository.findByCityId(request.cityId()).stream()
                        .filter(candidate -> candidate.getUsername().equalsIgnoreCase(request.username()))
                        .findFirst()
                        .orElseGet(() -> {
                            City city = lookup.city(request.cityId());
                            AppUser created = new AppUser();
                            created.setCity(city);
                            created.setUsername(request.username());
                            created.setDisplayName(request.displayName() == null || request.displayName().isBlank()
                                    ? request.username()
                                    : request.displayName());
                            created.setRole(UserRole.CITIZEN);
                            created.setPasswordConfigured(false);
                            created.setPasswordHash(passwordService.hash("pending-first-access"));
                            return created;
                        }));
        if (Boolean.TRUE.equals(user.getPasswordConfigured())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account already activated");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (request.displayName() != null && !request.displayName().isBlank()) {
            user.setDisplayName(request.displayName());
        }
        user.setPasswordHash(passwordService.hash(request.password()));
        user.setPasswordConfigured(true);
        AppUser saved = appUserRepository.save(user);
        activityLogService.record(saved, "ACCOUNT_ACTIVATED", saved.getUsername() + " a active son compte");
        return new AuthLoginResponse(toUserResponse(saved), toCityResponse(saved.getCity()));
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    private UserResponse toUserResponse(AppUser user) {
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

    private CityResponse toCityResponse(City city) {
        return new CityResponse(
                city.getId(),
                city.getName(),
                city.getApiUrl(),
                city.getCode(),
                city.getCreatedAt()
        );
    }
}
