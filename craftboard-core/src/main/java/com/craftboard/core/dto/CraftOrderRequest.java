package com.craftboard.core.dto;

import com.craftboard.core.enums.OrderStatus;
import java.util.List;

/**
 * DTO partage pour transporter des donnees entre le serveur et le desktop.
 */
public record CraftOrderRequest(
        Long cityId,
        Long createdBy,
        Long poleId,
        OrderStatus status,
        List<OrderItemRequest> items
) {
}
