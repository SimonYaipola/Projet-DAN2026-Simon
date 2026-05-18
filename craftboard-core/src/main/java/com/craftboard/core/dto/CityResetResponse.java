package com.craftboard.core.dto;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public record CityResetResponse(
        Long cityId,
        String cityName,
        String message
) {
}
