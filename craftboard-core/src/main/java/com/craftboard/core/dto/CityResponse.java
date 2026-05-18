package com.craftboard.core.dto;

import java.time.LocalDateTime;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public record CityResponse(
        Long id,
        String name,
        String apiUrl,
        String code,
        LocalDateTime createdAt
) {
}
