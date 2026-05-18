package com.craftboard.server.controller;

import com.craftboard.core.dto.QuestDeliveryRequest;
import com.craftboard.core.dto.QuestRequest;
import com.craftboard.core.dto.QuestResponse;
import com.craftboard.server.service.QuestService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controleur REST expose par le serveur CraftBoard.
 */
@RestController
@RequestMapping("/api/quests")
public class QuestController {

    private final QuestService service;

    public QuestController(QuestService service) {
        this.service = service;
    }

    @GetMapping
    public List<QuestResponse> getAll(@RequestParam(name = "cityId", required = false) Long cityId) {
        return cityId == null ? service.findAll() : service.findByCity(cityId);
    }

    @GetMapping("/{id}")
    public QuestResponse getById(@PathVariable(name = "id") Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestResponse create(@RequestBody QuestRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public QuestResponse update(@PathVariable(name = "id") Long id, @RequestBody QuestRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/deliveries")
    public QuestResponse deliver(
            @PathVariable(name = "id") Long id,
            @RequestBody QuestDeliveryRequest request) {
        return service.deliver(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable(name = "id") Long id) {
        service.delete(id);
    }
}
