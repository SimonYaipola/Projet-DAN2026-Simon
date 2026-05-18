package com.craftboard.core.dto;

import com.craftboard.core.enums.UserRole;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public record UserUpdateRequest(
        Long cityId,
        String username,
        String password,
        UserRole role,
        String displayName
) {
}
