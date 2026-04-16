package com.craftboard.server.controller;

import com.craftboard.server.dto.AppUserResponse;
import com.craftboard.server.dto.CreateAppUserRequest;
import com.craftboard.server.entity.AppUser;
import com.craftboard.server.entity.City;
import com.craftboard.server.repository.AppUserRepository;
import com.craftboard.server.repository.CityRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final AppUserRepository appUserRepository;
    private final CityRepository cityRepository;

    public UserController(
            AppUserRepository appUserRepository,
            CityRepository cityRepository) {
        this.appUserRepository = appUserRepository;
        this.cityRepository = cityRepository;
    }

    @GetMapping
    public List<AppUserResponse> getAll() {
        return appUserRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/city/{cityId}")
    public List<AppUserResponse> getByCity(@PathVariable Long cityId) {
        return appUserRepository.findByCityId(cityId).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping
    public AppUserResponse create(@RequestBody CreateAppUserRequest request) {
        City city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "City not found"));

        AppUser user = new AppUser();
        user.setCity(city);
        user.setUsername(request.getUsername());
        user.setPasswordHash(request.getPasswordHash());
        user.setRole(request.getRole());
        user.setDisplayName(request.getDisplayName());

        AppUser savedUser = appUserRepository.save(user);
        return toResponse(savedUser);
    }

    private AppUserResponse toResponse(AppUser user) {
        return new AppUserResponse(
                user.getId(),
                user.getCity().getId(),
                user.getUsername(),
                user.getRole(),
                user.getDisplayName(),
                user.getCreatedAt()
        );
    }
}