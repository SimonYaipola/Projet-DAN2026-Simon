package com.craftboard.core.dto;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public record OrderItemResponse(
        Long id,
        String category,
        String tier,
        String rarity,
        Integer quantity,
        String toolType,
        String equipmentType,
        String armorMaterial
) {
}
