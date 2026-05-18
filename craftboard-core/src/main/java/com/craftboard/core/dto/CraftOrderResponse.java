package com.craftboard.core.dto;

import com.craftboard.core.enums.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public record CraftOrderResponse(
        Long id,
        Long cityId,
        Long createdBy,
        String createdByUsername,
        Long assignedTo,
        String assignedToUsername,
        Long poleId,
        String poleName,
        OrderStatus status,
        String notes,
        LocalDateTime createdAt,
        List<OrderItemResponse> items
) {
}
