package com.craftboard.core.dto;

import com.craftboard.core.enums.QuestStatus;
import com.craftboard.core.enums.QuestRecurrence;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public record QuestResponse(
        Long id,
        Long cityId,
        String title,
        String description,
        QuestStatus status,
        QuestRecurrence recurrence,
        String resourceName,
        Integer targetQuantity,
        Integer deliveredQuantity,
        Integer remainingQuantity,
        Long createdBy,
        LocalDateTime createdAt,
        List<QuestDeliveryResponse> deliveries
) {
}
