package com.craftboard.server.controller;

import com.craftboard.core.dto.EquipmentRequest;
import com.craftboard.core.dto.EquipmentResponse;
import com.craftboard.core.dto.PlayerEquipmentResponse;
import com.craftboard.server.service.BitjitaEquipmentService;
import com.craftboard.server.service.EquipmentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controleur REST expose par le serveur CraftBoard.
 */
@RestController
@RequestMapping("/api/equipments")
public class EquipmentController {

    private final EquipmentService service;
    private final BitjitaEquipmentService bitjitaEquipmentService;

    public EquipmentController(EquipmentService service, BitjitaEquipmentService bitjitaEquipmentService) {
        this.service = service;
        this.bitjitaEquipmentService = bitjitaEquipmentService;
    }

    @GetMapping
    public List<EquipmentResponse> getAll(@RequestParam(name = "cityId", required = false) Long cityId) {
        return cityId == null ? service.findAll() : service.findByCity(cityId);
    }

    @GetMapping("/users/{userId}/bitjita-equipped")
    public PlayerEquipmentResponse getBitjitaEquipped(@PathVariable(name = "userId") Long userId) {
        return bitjitaEquipmentService.findEquippedToolsAndArmor(userId);
    }

    @GetMapping("/{id}")
    public EquipmentResponse getById(@PathVariable(name = "id") Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EquipmentResponse create(@RequestBody EquipmentRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public EquipmentResponse update(@PathVariable(name = "id") Long id, @RequestBody EquipmentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable(name = "id") Long id) {
        service.delete(id);
    }
}
