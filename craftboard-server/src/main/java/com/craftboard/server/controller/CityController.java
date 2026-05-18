package com.craftboard.server.controller;

import com.craftboard.core.dto.CityRequest;
import com.craftboard.core.dto.CityResetRequest;
import com.craftboard.core.dto.CityResetResponse;
import com.craftboard.core.dto.CityResponse;
import com.craftboard.server.service.CityService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controleur REST expose par le serveur CraftBoard.
 */
@RestController
@RequestMapping("/api/cities")
public class CityController {

    private final CityService service;

    public CityController(CityService service) {
        this.service = service;
    }

    @GetMapping
    public List<CityResponse> getAll(@RequestParam(name = "importedOnly", required = false) Boolean importedOnly) {
        return Boolean.TRUE.equals(importedOnly)
                ? service.findImportedFromBitjita()
                : service.findAll();
    }

    @GetMapping("/{id}")
    public CityResponse getById(@PathVariable(name = "id") Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CityResponse create(@RequestBody CityRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CityResponse update(@PathVariable(name = "id") Long id, @RequestBody CityRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable(name = "id") Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/reset")
    public CityResetResponse resetImportedCity(
            @PathVariable(name = "id") Long id,
            @RequestBody CityResetRequest request) {
        return service.resetImportedCity(id, request.adminPassword());
    }
}
