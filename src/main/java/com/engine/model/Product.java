package com.engine.model;

import com.engine.exception.InsufficientInventoryException;
import com.engine.exception.ValidationException;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Abstract domain model for commercial products.
 * Demonstrates OOP inheritance, encapsulation, and template/polymorphic pricing methods.
 */
public abstract class Product extends BaseEntity {
    private final String sku;
    private String name;
    private BigDecimal basePrice;
    private int stockQuantity;

    protected Product(String id, String sku, String name, BigDecimal basePrice, int stockQuantity) {
        super(id);
        this.sku = sku;
        this.name = name;
        this.basePrice = basePrice;
        this.stockQuantity = stockQuantity;
        validate();
    }

    protected Product(String sku, String name, BigDecimal basePrice, int stockQuantity) {
        super();
        this.sku = sku;
        this.name = name;
        this.basePrice = basePrice;
        this.stockQuantity = stockQuantity;
        validate();
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Product name cannot be empty");
        }
        this.name = name.trim();
        touch();
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public void setBasePrice(BigDecimal basePrice) {
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Base price cannot be null or negative");
        }
        this.basePrice = basePrice;
        touch();
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    /**
     * Reduces available stock safely with domain verification.
     * Throws checked InsufficientInventoryException if requested qty exceeds stock.
     */
    public synchronized void reduceStock(int quantity) throws InsufficientInventoryException {
        if (quantity <= 0) {
            throw new ValidationException("Quantity to reduce must be greater than zero");
        }
        if (this.stockQuantity < quantity) {
            throw new InsufficientInventoryException(this.sku, quantity, this.stockQuantity);
        }
        this.stockQuantity -= quantity;
        touch();
    }

    /**
     * Replenishes product stock.
     */
    public synchronized void restock(int quantity) {
        if (quantity <= 0) {
            throw new ValidationException("Quantity to restock must be greater than zero");
        }
        this.stockQuantity += quantity;
        touch();
    }

    public boolean isAvailable(int requestedQty) {
        return requestedQty > 0 && this.stockQuantity >= requestedQty;
    }

    /**
     * Polymorphic method implemented by specialized product types.
     * Computes the final customer price including category-specific surcharges, taxes, or shipping.
     */
    public abstract BigDecimal calculateFinalPrice();

    /**
     * Identifies the category/type of product.
     */
    public abstract String getProductType();

    @Override
    public void validate() {
        if (sku == null || sku.isBlank()) {
            throw new ValidationException("Product SKU cannot be null or blank");
        }
        if (name == null || name.isBlank()) {
            throw new ValidationException("Product name cannot be null or blank");
        }
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Base price cannot be null or negative");
        }
        if (stockQuantity < 0) {
            throw new ValidationException("Stock quantity cannot be negative");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product product)) return false;
        return Objects.equals(sku, product.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku);
    }
}
