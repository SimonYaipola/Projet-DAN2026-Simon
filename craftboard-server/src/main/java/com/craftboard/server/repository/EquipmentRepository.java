package com.craftboard.server.repository;

import com.craftboard.server.entity.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository Spring Data pour acceder aux donnees persistantes.
 */
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findByCityId(Long cityId);
}
