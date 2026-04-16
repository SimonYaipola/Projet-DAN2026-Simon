package com.craftboard.server.controller;

import com.craftboard.server.entity.City;
import com.craftboard.server.repository.CityRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
public class CityController {

    private final CityRepository repository;

    public CityController(CityRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<City> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public City getById(@PathVariable Long id) {
        return repository.findById(id).orElse(null);
    }

    @PostMapping
    public City create(@RequestBody City city) {
        return repository.save(city);
    }
}