package com.craftboard.server.repository;

import com.craftboard.server.entity.Quest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository Spring Data pour acceder aux donnees persistantes.
 */
public interface QuestRepository extends JpaRepository<Quest, Long> {

    List<Quest> findByCityId(Long cityId);
}
