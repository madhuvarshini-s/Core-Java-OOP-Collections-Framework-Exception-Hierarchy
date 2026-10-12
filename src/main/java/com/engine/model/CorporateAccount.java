package com.engine.model;

import com.engine.exception.CreditLimitExceededException;
import com.engine.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Concrete corporate business account featuring an authorized credit line,
 * federal tax identifier, and enterprise transaction processing fees.
 */
public class CorporateAccount extends Account {
    private BigDecimal creditLimit;
    private String taxId;

    public CorporateAccount(String accountHolder, String email, BigDecimal initialBalance,
                            BigDecimal creditLimit, String taxId) {
        super(accountHolder, email, initialBalance);
        this.creditLimit = creditLimit != null ? creditLimit.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        this.taxId = taxId;
        validateCorporateAttributes();
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        if (creditLimit == null || creditLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Credit limit cannot be null or negative");
        }
        this.creditLimit = creditLimit.setScale(2, RoundingMode.HALF_UP);
        touch();
    }

    public String getTaxId() {
        return taxId;
    }

    public void setTaxId(String taxId) {
        if (taxId == null || taxId.isBlank()) {
            throw new ValidationException("Tax ID cannot be blank");
        }
        this.taxId = taxId.trim();
        touch();
    }

    private void validateCorporateAttributes() {
        if (creditLimit == null || creditLimit.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Credit limit cannot be negative");
        }
        if (taxId == null || taxId.isBlank()) {
            throw new ValidationException("Corporate tax ID is required");
        }
    }

    @Override
    public synchronized void withdraw(BigDecimal amount) throws CreditLimitExceededException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Withdrawal amount must be positive: " + amount);
        }
        BigDecimal fee = calculateFee(amount);
        BigDecimal totalRequired = amount.add(fee);
        BigDecimal maxSpendable = getAvailableFunds();

        if (maxSpendable.compareTo(totalRequired) < 0) {
            throw new CreditLimitExceededException(getId(), totalRequired, maxSpendable);
        }

        // Deducts from balance; balance can legally go negative up to negative creditLimit
        setBalance(getBalance().subtract(totalRequired));
    }

    @Override
    public BigDecimal getAvailableFunds() {
        return getBalance().add(creditLimit).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public BigDecimal calculateFee(BigDecimal amount) {
        // Corporate processing fee: 1.0%
        return amount.multiply(new BigDecimal("0.01")).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.CORPORATE;
    }

    @Override
    public String toString() {
        return "CorporateAccount{" +
                "id='" + getId() + '\'' +
                ", holder='" + getAccountHolder() + '\'' +
                ", email='" + getEmail() + '\'' +
                ", balance=" + getBalance() +
                ", creditLimit=" + creditLimit +
                ", taxId='" + taxId + '\'' +
                '}';
    }
}
