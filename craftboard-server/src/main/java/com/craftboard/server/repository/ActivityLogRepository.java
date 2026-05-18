package com.craftboard.server.repository;

import com.craftboard.server.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository Spring Data pour acceder aux donnees persistantes.
 */
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findByCityIdOrderByCreatedAtDesc(Long cityId);
}
