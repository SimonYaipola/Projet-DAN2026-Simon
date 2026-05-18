package com.craftboard.server.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controleur REST expose par le serveur CraftBoard.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public String health() {
        return "Le serveur tourne !";
    }
}