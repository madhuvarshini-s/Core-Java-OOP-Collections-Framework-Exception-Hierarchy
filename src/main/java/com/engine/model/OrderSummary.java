package com.engine.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Immutable DTO record summarizing an order for lightweight querying and reporting.
 */
public record OrderSummary(
        String orderId,
        String accountId,
        BigDecimal totalAmount,
        OrderStatus status,
        Instant placedAt,
        int itemCount
) {
}
