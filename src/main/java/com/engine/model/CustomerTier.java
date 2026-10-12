package com.engine.model;

import java.math.BigDecimal;

/**
 * Customer tiers offering tiered discounts and reward points multipliers.
 */
public enum CustomerTier {
    STANDARD(BigDecimal.ZERO, 1.0),
    SILVER(new BigDecimal("0.05"), 1.25),
    GOLD(new BigDecimal("0.10"), 1.5),
    PLATINUM(new BigDecimal("0.15"), 2.0);

    private final BigDecimal discountRate;
    private final double pointsMultiplier;

    CustomerTier(BigDecimal discountRate, double pointsMultiplier) {
        this.discountRate = discountRate;
        this.pointsMultiplier = pointsMultiplier;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public double getPointsMultiplier() {
        return pointsMultiplier;
    }
}
