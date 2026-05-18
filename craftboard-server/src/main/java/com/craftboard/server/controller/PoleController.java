package com.craftboard.server.controller;

import com.craftboard.core.dto.PoleRequest;
import com.craftboard.core.dto.PoleResponse;
import com.craftboard.server.service.PoleService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controleur REST expose par le serveur CraftBoard.
 */
@RestController
@RequestMapping("/api/poles")
public class PoleController {

    private final PoleService service;

    public PoleController(PoleService service) {
        this.service = service;
    }

    @GetMapping
    public List<PoleResponse> getAll(@RequestParam(name = "cityId", required = false) Long cityId) {
        return cityId == null ? service.findAll() : service.findByCity(cityId);
    }

    @GetMapping("/{id}")
    public PoleResponse getById(@PathVariable(name = "id") Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PoleResponse create(@RequestBody PoleRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public PoleResponse update(@PathVariable(name = "id") Long id, @RequestBody PoleRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable(name = "id") Long id) {
        service.delete(id);
    }
}
