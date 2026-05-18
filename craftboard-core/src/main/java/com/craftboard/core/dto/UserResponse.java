package com.craftboard.core.dto;

import com.craftboard.core.enums.UserRole;
import java.time.LocalDateTime;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public record UserResponse(
        Long id,
        Long cityId,
        String username,
        UserRole role,
        String displayName,
        String playerEntityId,
        Boolean passwordConfigured,
        LocalDateTime createdAt
) {
}
