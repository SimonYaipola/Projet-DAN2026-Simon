package com.craftboard.core.dto;

import com.craftboard.core.enums.EquipmentCategory;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public record EquippedItemResponse(
        String source,
        String slot,
        Long itemId,
        String name,
        EquipmentCategory category,
        Integer tier,
        String rarity,
        String tag,
        String iconAssetName,
        String imageUrl
) {
}
