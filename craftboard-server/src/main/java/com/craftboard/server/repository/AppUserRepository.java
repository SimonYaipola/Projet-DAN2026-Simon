package com.craftboard.server.repository;

import com.craftboard.server.entity.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository Spring Data pour acceder aux donnees persistantes.
 */
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByUsername(String username);

    List<AppUser> findByCityId(Long cityId);

    Optional<AppUser> findByCityIdAndUsername(Long cityId, String username);
}