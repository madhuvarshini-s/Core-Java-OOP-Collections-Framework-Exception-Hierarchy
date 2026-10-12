package com.engine.model;

import com.engine.exception.CreditLimitExceededException;
import com.engine.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Concrete retail customer account with reward points program and tier-based discounts.
 */
public class CustomerAccount extends Account {
    private CustomerTier tier;
    private BigDecimal rewardPoints;

    public CustomerAccount(String accountHolder, String email, BigDecimal initialBalance, CustomerTier tier) {
        super(accountHolder, email, initialBalance);
        this.tier = Objects.requireNonNull(tier, "CustomerTier cannot be null");
        this.rewardPoints = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public CustomerTier getTier() {
        return tier;
    }

    public void setTier(CustomerTier tier) {
        this.tier = Objects.requireNonNull(tier, "CustomerTier cannot be null");
        touch();
    }

    public BigDecimal getRewardPoints() {
        return rewardPoints;
    }

    public synchronized void awardPoints(BigDecimal amountSpent) {
        if (amountSpent != null && amountSpent.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal earned = amountSpent.multiply(BigDecimal.valueOf(tier.getPointsMultiplier()))
                    .setScale(2, RoundingMode.HALF_UP);
            this.rewardPoints = this.rewardPoints.add(earned);
            touch();
        }
    }

    public synchronized void redeemPoints(BigDecimal points) {
        if (points == null || points.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Points to redeem must be greater than zero");
        }
        if (this.rewardPoints.compareTo(points) < 0) {
            throw new ValidationException(String.format(
                    "Insufficient reward points: requested %s, available %s", points, this.rewardPoints));
        }
        this.rewardPoints = this.rewardPoints.subtract(points).setScale(2, RoundingMode.HALF_UP);
        touch();
    }

    @Override
    public synchronized void withdraw(BigDecimal amount) throws CreditLimitExceededException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Withdrawal amount must be positive: " + amount);
        }
        BigDecimal fee = calculateFee(amount);
        BigDecimal totalRequired = amount.add(fee);

        if (getBalance().compareTo(totalRequired) < 0) {
            throw new CreditLimitExceededException(getId(), totalRequired, getBalance());
        }

        setBalance(getBalance().subtract(totalRequired));
    }

    @Override
    public BigDecimal getAvailableFunds() {
        return getBalance();
    }

    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        // Retail customers have zero flat fees; platinum tier has 0 fee, others pay nominal 0.5% only over $1000
        if (tier == CustomerTier.PLATINUM || amount.compareTo(BigDecimal.valueOf(1000)) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.multiply(new BigDecimal("0.005")).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.CUSTOMER;
    }

    @Override
    public String toString() {
        return "CustomerAccount{" +
                "id='" + getId() + '\'' +
                ", holder='" + getAccountHolder() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", balance=" + getBalance() +
                ", tier=" + tier +
                ", rewardPoints=" + rewardPoints +
                '}';
    }
}
