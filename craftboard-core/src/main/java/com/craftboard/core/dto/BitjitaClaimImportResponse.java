package com.craftboard.core.dto;

import java.util.List;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public record BitjitaClaimImportResponse(
        CityResponse city,
        BitjitaClaimSummary claim,
        List<BitjitaMemberSummary> members,
        int memberCount,
        int citizenCount
) {
}
