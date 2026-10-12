package com.engine.model;

import java.math.BigDecimal;

/**
 * Immutable aggregation DTO summarizing inventory assets and stock health.
 */
public record InventoryReport(
        int totalProducts,
        int lowStockCount,
        BigDecimal totalCatalogValue
) {
}
