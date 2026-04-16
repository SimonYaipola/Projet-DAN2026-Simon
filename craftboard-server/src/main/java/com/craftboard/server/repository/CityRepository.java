package com.craftboard.server.repository;

import com.craftboard.server.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CityRepository extends JpaRepository<City, Long> {

    Optional<City> findByCode(String code);
}