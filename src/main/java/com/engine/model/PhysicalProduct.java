package com.engine.model;

import com.engine.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Concrete Product specialization representing tangible goods requiring physical logistics.
 */
public class PhysicalProduct extends Product {
    private double weightKg;
    private Dimensions dimensions;
    private boolean fragile;

    public PhysicalProduct(String sku, String name, BigDecimal basePrice, int stockQuantity,
                           double weightKg, Dimensions dimensions, boolean fragile) {
        super(sku, name, basePrice, stockQuantity);
        this.weightKg = weightKg;
        this.dimensions = Objects.requireNonNull(dimensions, "Dimensions cannot be null");
        this.fragile = fragile;
        validatePhysicalAttributes();
    }

    public double getWeightKg() {
        return weightKg;
    }

    public Dimensions getDimensions() {
        return dimensions;
    }

    public boolean isFragile() {
        return fragile;
    }

    public void setWeightKg(double weightKg) {
        if (weightKg <= 0) {
            throw new ValidationException("Weight must be greater than zero");
        }
        this.weightKg = weightKg;
        touch();
    }

    public void setDimensions(Dimensions dimensions) {
        this.dimensions = Objects.requireNonNull(dimensions, "Dimensions cannot be null");
        touch();
    }

    public void setFragile(boolean fragile) {
        this.fragile = fragile;
        touch();
    }

    private void validatePhysicalAttributes() {
        if (weightKg <= 0) {
            throw new ValidationException("Weight must be greater than zero: " + weightKg);
        }
    }

    /**
     * Shipping cost based on dimensional weight and fragility surcharge.
     */
    public BigDecimal calculateShippingCost() {
        // Base rate: $2.50 per kg + $5 fragile surcharge if applicable
        BigDecimal weightCharge = BigDecimal.valueOf(weightKg)
                .multiply(BigDecimal.valueOf(2.50))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal fragileCharge = fragile ? BigDecimal.valueOf(5.00) : BigDecimal.ZERO;
        return weightCharge.add(fragileCharge);
    }

    @Override
    public BigDecimal calculateFinalPrice() {
        // Base price + shipping cost
        return getBasePrice().add(calculateShippingCost()).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getProductType() {
        return "PHYSICAL";
    }

    @Override
    public String toString() {
        return "PhysicalProduct{" +
                "sku='" + getSku() + '\'' +
                ", name='" + getName() + '\'' +
                ", basePrice=" + getBasePrice() +
                ", stockQuantity=" + getStockQuantity() +
                ", weightKg=" + weightKg +
                ", dimensions=" + dimensions +
                ", fragile=" + fragile +
                '}';
    }
}
