package com.craftboard.server.controller;

import com.craftboard.core.dto.ActivityLogRequest;
import com.craftboard.core.dto.ActivityLogResponse;
import com.craftboard.server.service.ActivityLogService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controleur REST expose par le serveur CraftBoard.
 */
@RestController
@RequestMapping("/api/activity-logs")
public class ActivityLogController {

    private final ActivityLogService service;

    public ActivityLogController(ActivityLogService service) {
        this.service = service;
    }

    @GetMapping
    public List<ActivityLogResponse> getAll(@RequestParam(name = "cityId", required = false) Long cityId) {
        return cityId == null ? service.findAll() : service.findByCity(cityId);
    }

    @GetMapping("/{id}")
    public ActivityLogResponse getById(@PathVariable(name = "id") Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityLogResponse create(@RequestBody ActivityLogRequest request) {
        return service.create(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable(name = "id") Long id) {
        service.delete(id);
    }
}
