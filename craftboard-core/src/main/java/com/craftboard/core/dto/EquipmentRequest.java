package com.craftboard.core.dto;

import com.craftboard.core.enums.EquipmentCategory;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public record EquipmentRequest(
        Long cityId,
        Long ownerId,
        Long poleId,
        String name,
        EquipmentCategory category,
        String tier,
        String rarity,
        Integer conditionValue
) {
}
