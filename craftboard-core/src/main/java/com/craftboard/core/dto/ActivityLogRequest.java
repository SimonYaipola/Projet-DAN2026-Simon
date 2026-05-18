package com.craftboard.core.dto;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public record ActivityLogRequest(
        Long cityId,
        Long userId,
        String actionType,
        String description
) {
}
