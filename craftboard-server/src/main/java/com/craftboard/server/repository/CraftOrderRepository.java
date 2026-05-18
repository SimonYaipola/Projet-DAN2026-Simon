package com.craftboard.server.repository;

import com.craftboard.server.entity.CraftOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository Spring Data pour acceder aux donnees persistantes.
 */
public interface CraftOrderRepository extends JpaRepository<CraftOrder, Long> {

    List<CraftOrder> findByCityId(Long cityId);
}
