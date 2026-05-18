package com.craftboard.server.controller;

import com.craftboard.core.dto.AccountActivationRequest;
import com.craftboard.core.dto.AuthLoginRequest;
import com.craftboard.core.dto.AuthLoginResponse;
import com.craftboard.core.dto.SetupAdminRequest;
import com.craftboard.server.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Controleur REST expose par le serveur CraftBoard.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public AuthLoginResponse login(@RequestBody AuthLoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/cities/{cityId}/initial-admin")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthLoginResponse createInitialAdmin(
            @PathVariable(name = "cityId") Long cityId,
            @RequestBody SetupAdminRequest request) {
        return authService.createInitialAdmin(cityId, request);
    }

    @PostMapping("/activate")
    public AuthLoginResponse activate(@RequestBody AccountActivationRequest request) {
        return authService.activateAccount(request);
    }
}
