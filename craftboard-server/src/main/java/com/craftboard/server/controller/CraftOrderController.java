package com.craftboard.server.controller;

import com.craftboard.core.dto.CraftOrderRequest;
import com.craftboard.core.dto.CraftOrderResponse;
import com.craftboard.core.dto.OrderActionRequest;
import com.craftboard.server.service.CraftOrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controleur REST expose par le serveur CraftBoard.
 */
@RestController
@RequestMapping("/api/orders")
public class CraftOrderController {

    private final CraftOrderService service;

    public CraftOrderController(CraftOrderService service) {
        this.service = service;
    }

    @GetMapping
    public List<CraftOrderResponse> getAll(@RequestParam(name = "cityId", required = false) Long cityId) {
        return cityId == null ? service.findAll() : service.findByCity(cityId);
    }

    @GetMapping("/{id}")
    public CraftOrderResponse getById(@PathVariable(name = "id") Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CraftOrderResponse create(@RequestBody CraftOrderRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public CraftOrderResponse update(@PathVariable(name = "id") Long id, @RequestBody CraftOrderRequest request) {
        return service.update(id, request);
    }

    @PostMapping("/{id}/assign")
    public CraftOrderResponse assignToSelf(
            @PathVariable(name = "id") Long id,
            @RequestBody OrderActionRequest request) {
        return service.assignToSelf(id, request);
    }

    @PostMapping("/{id}/complete")
    public CraftOrderResponse complete(
            @PathVariable(name = "id") Long id,
            @RequestBody OrderActionRequest request) {
        return service.complete(id, request);
    }

    @PostMapping("/{id}/cancel")
    public CraftOrderResponse cancel(
            @PathVariable(name = "id") Long id,
            @RequestBody OrderActionRequest request) {
        return service.cancel(id, request);
    }

    @PostMapping("/{id}/reopen")
    public CraftOrderResponse reopen(
            @PathVariable(name = "id") Long id,
            @RequestBody OrderActionRequest request) {
        return service.reopen(id, request);
    }

    @PatchMapping("/{id}/notes")
    public CraftOrderResponse updateNotes(
            @PathVariable(name = "id") Long id,
            @RequestBody OrderActionRequest request) {
        return service.updateNotes(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable(name = "id") Long id) {
        service.delete(id);
    }
}
