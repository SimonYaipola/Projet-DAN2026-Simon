package com.craftboard.core.dto;

import java.time.LocalDateTime;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public record ActivityLogResponse(
        Long id,
        Long cityId,
        Long userId,
        String username,
        String actionType,
        String description,
        LocalDateTime createdAt
) {
}
