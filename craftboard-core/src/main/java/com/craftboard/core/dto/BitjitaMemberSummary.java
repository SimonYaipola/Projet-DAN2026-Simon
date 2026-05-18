package com.craftboard.core.dto;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public record BitjitaMemberSummary(
        String playerEntityId,
        String userName,
        Integer inventoryPermission,
        Integer buildPermission,
        Integer officerPermission,
        Integer coOwnerPermission,
        Integer highestLevel,
        Integer totalLevel
) {
}
