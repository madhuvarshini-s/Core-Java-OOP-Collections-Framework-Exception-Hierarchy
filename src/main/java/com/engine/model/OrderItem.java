package com.engine.model;

import com.engine.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Immutable value record representing an ordered item line.
 */
public record OrderItem(String productSku, String productName, int quantity, BigDecimal unitPrice) {
    public OrderItem {
        if (productSku == null || productSku.isBlank()) {
            throw new ValidationException("Product SKU cannot be blank");
        }
        if (productName == null || productName.isBlank()) {
            throw new ValidationException("Product name cannot be blank");
        }
        if (quantity <= 0) {
            throw new ValidationException("Quantity must be strictly positive: " + quantity);
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Unit price cannot be null or negative");
        }
        unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Line item subtotal = quantity * unitPrice.
     */
    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    }
}
