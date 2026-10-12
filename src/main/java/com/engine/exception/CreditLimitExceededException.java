package com.engine.exception;

import java.math.BigDecimal;

/**
 * Checked exception thrown when an account cannot fulfill an order or withdrawal
 * due to insufficient balance or exceeding assigned credit limit.
 */
public class CreditLimitExceededException extends BusinessEngineException {
    private final String accountId;
    private final BigDecimal attemptedAmount;
    private final BigDecimal availableLimit;

    public CreditLimitExceededException(String accountId, BigDecimal attemptedAmount, BigDecimal availableLimit) {
        super(ErrorCode.CREDIT_LIMIT_EXCEEDED,
                String.format("Account '%s' exceeded credit/balance limit: attempted %s, available %s",
                        accountId, attemptedAmount, availableLimit));
        this.accountId = accountId;
        this.attemptedAmount = attemptedAmount;
        this.availableLimit = availableLimit;
    }

    public String getAccountId() {
        return accountId;
    }

    public BigDecimal getAttemptedAmount() {
        return attemptedAmount;
    }

    public BigDecimal getAvailableLimit() {
        return availableLimit;
    }
}
