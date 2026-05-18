package com.craftboard.core.dto;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public record AuthLoginRequest(
        Long cityId,
        String username,
        String password
) {
}
