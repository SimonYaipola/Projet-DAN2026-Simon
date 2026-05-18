package com.craftboard.server.repository;

import com.craftboard.server.entity.Pole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data pour acceder aux donnees persistantes.
 */
public interface PoleRepository extends JpaRepository<Pole, Long> {

    List<Pole> findByCityId(Long cityId);

    Optional<Pole> findByCityIdAndName(Long cityId, String name);
}
