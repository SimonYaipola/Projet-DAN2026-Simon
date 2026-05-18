package com.craftboard.core.dto;

import com.craftboard.core.enums.EquipmentCategory;
import java.time.LocalDateTime;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public record EquipmentResponse(
        Long id,
        Long cityId,
        Long ownerId,
        Long poleId,
        String name,
        EquipmentCategory category,
        String tier,
        String rarity,
        String bitjitaItemId,
        String slot,
        String iconAssetName,
        String imageUrl,
        String rarityStr,
        Integer conditionValue,
        LocalDateTime createdAt
) {
}
