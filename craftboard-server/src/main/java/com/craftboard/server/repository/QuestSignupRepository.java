package com.craftboard.server.repository;

import com.craftboard.server.entity.QuestSignup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data pour acceder aux donnees persistantes.
 */
public interface QuestSignupRepository extends JpaRepository<QuestSignup, Long> {

    Optional<QuestSignup> findByQuestIdAndUserId(Long questId, Long userId);

    List<QuestSignup> findByQuestCityId(Long cityId);
}
