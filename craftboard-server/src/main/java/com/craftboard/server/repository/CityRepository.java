package com.craftboard.server.repository;

import com.craftboard.server.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data pour acceder aux donnees persistantes.
 */
public interface CityRepository extends JpaRepository<City, Long> {

    Optional<City> findByCode(String code);

    List<City> findByApiUrlStartingWith(String apiUrlPrefix);
}
