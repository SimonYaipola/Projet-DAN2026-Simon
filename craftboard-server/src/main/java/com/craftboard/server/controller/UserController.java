package com.craftboard.server.controller;

import com.craftboard.core.dto.AdminUserUpdateRequest;
import com.craftboard.core.dto.UserCreateRequest;
import com.craftboard.core.dto.UserResponse;
import com.craftboard.core.dto.UserUpdateRequest;
import com.craftboard.server.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controleur REST expose par le serveur CraftBoard.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    public List<UserResponse> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable(name = "id") Long id) {
        return service.findById(id);
    }

    @GetMapping("/city/{cityId}")
    public List<UserResponse> getByCity(@PathVariable(name = "cityId") Long cityId) {
        return service.findByCity(cityId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@RequestBody UserCreateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable(name = "id") Long id, @RequestBody UserUpdateRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/admin")
    public UserResponse adminUpdate(
            @PathVariable(name = "id") Long id,
            @RequestBody AdminUserUpdateRequest request) {
        return service.adminUpdate(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable(name = "id") Long id) {
        service.delete(id);
    }
}
