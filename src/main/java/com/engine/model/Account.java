package com.engine.model;

import com.engine.exception.CreditLimitExceededException;
import com.engine.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Abstract financial account providing fundamental balance tracking,
 * thread-safe deposits, and polymorphic withdrawal and fee rules.
 */
public abstract class Account extends BaseEntity {
    private String accountHolder;
    private final String email;
    private BigDecimal balance;

    protected Account(String id, String accountHolder, String email, BigDecimal initialBalance) {
        super(id);
        this.accountHolder = accountHolder;
        this.email = email;
        this.balance = initialBalance != null ? initialBalance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        validate();
    }

    protected Account(String accountHolder, String email, BigDecimal initialBalance) {
        super();
        this.accountHolder = accountHolder;
        this.email = email;
        this.balance = initialBalance != null ? initialBalance.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        validate();
    }

    public String getAccountHolder() {
        return accountHolder;
    }

    public void setAccountHolder(String accountHolder) {
        if (accountHolder == null || accountHolder.isBlank()) {
            throw new ValidationException("Account holder cannot be empty");
        }
        this.accountHolder = accountHolder.trim();
        touch();
    }

    public String getEmail() {
        return email;
    }

    public synchronized BigDecimal getBalance() {
        return balance;
    }

    protected synchronized void setBalance(BigDecimal newBalance) {
        this.balance = newBalance.setScale(2, RoundingMode.HALF_UP);
        touch();
    }

    /**
     * Deposits funds into the account.
     */
    public synchronized void deposit(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Deposit amount must be positive: " + amount);
        }
        this.balance = this.balance.add(amount).setScale(2, RoundingMode.HALF_UP);
        touch();
    }

    /**
     * Polymorphic withdrawal logic enforced by account subtype.
     * Throws checked CreditLimitExceededException when funds/credit are insufficient.
     */
    public abstract void withdraw(BigDecimal amount) throws CreditLimitExceededException;

    /**
     * Total spendable funds (balance plus any authorized credit line).
     */
    public abstract BigDecimal getAvailableFunds();

    /**
     * Transaction processing fee based on account type.
     */
    public abstract BigDecimal calculateFee(BigDecimal amount);

    /**
     * Account classification enum.
     */
    public abstract AccountType getAccountType();

    @Override
    public void validate() {
        if (accountHolder == null || accountHolder.isBlank()) {
            throw new ValidationException("Account holder name cannot be null or blank");
        }
        if (email == null || !email.contains("@")) {
            throw new ValidationException("Valid email address required: " + email);
        }
        if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Initial balance cannot be negative");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Account account)) return false;
        return Objects.equals(email, account.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }
}
