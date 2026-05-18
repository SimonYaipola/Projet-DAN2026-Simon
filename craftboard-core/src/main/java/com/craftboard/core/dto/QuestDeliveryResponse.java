package com.craftboard.core.dto;

import java.time.LocalDateTime;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public record QuestDeliveryResponse(
        Long id,
        Long questId,
        Long userId,
        String username,
        Integer deliveredQuantity,
        LocalDateTime createdAt
) {
}
