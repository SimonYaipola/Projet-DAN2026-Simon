package com.craftboard.core.dto;

import com.craftboard.core.enums.QuestStatus;
import com.craftboard.core.enums.QuestRecurrence;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public record QuestRequest(
        Long cityId,
        String title,
        String description,
        QuestStatus status,
        QuestRecurrence recurrence,
        String resourceName,
        Integer targetQuantity,
        Long createdBy
) {
}
