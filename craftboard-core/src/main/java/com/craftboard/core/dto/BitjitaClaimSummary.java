package com.craftboard.core.dto;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public record BitjitaClaimSummary(
        String entityId,
        String name,
        String regionName,
        Integer tier,
        Integer numTiles,
        String ownerPlayerUsername
) {
}
