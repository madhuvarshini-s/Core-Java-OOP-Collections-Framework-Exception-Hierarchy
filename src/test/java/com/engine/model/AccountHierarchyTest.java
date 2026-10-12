package com.engine.model;

import com.engine.exception.CreditLimitExceededException;
import com.engine.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Account Hierarchy & Encapsulation Tests")
class AccountHierarchyTest {

    @Test
    @DisplayName("CustomerAccount manages balance, tier rewards, and fee-free standard withdrawals")
    void testCustomerAccountOperations() throws CreditLimitExceededException {
        CustomerAccount account = new CustomerAccount(
                "Jane Doe",
                "jane@example.com",
                new BigDecimal("500.00"),
                CustomerTier.GOLD
        );

        assertThat(account.getAccountType()).isEqualTo(AccountType.CUSTOMER);
        assertThat(account.getAvailableFunds()).isEqualByComparingTo("500.00");

        account.deposit(new BigDecimal("200.00"));
        assertThat(account.getBalance()).isEqualByComparingTo("700.00");

        account.withdraw(new BigDecimal("300.00"));
        assertThat(account.getBalance()).isEqualByComparingTo("400.00");

        // Reward points (Gold tier has 1.5 multiplier)
        account.awardPoints(new BigDecimal("100.00"));
        assertThat(account.getRewardPoints()).isEqualByComparingTo("150.00");

        account.redeemPoints(new BigDecimal("50.00"));
        assertThat(account.getRewardPoints()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("CustomerAccount throws CreditLimitExceededException when withdrawing more than balance")
    void testCustomerAccountOverdraftThrowsException() {
        CustomerAccount account = new CustomerAccount(
                "John Smith",
                "john@example.com",
                new BigDecimal("100.00"),
                CustomerTier.STANDARD
        );

        CreditLimitExceededException ex = assertThrows(
                CreditLimitExceededException.class,
                () -> account.withdraw(new BigDecimal("150.00"))
        );

        assertThat(ex.getAccountId()).isEqualTo(account.getId());
        assertThat(ex.getAttemptedAmount()).isEqualByComparingTo("150.00");
        assertThat(ex.getAvailableLimit()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("CorporateAccount permits spending into authorized credit limit with 1% processing fee")
    void testCorporateAccountCreditLimit() throws CreditLimitExceededException {
        CorporateAccount corp = new CorporateAccount(
                "Acme Corp",
                "finance@acme.com",
                new BigDecimal("1000.00"),
                new BigDecimal("5000.00"), // Credit line
                "TAX-998877"
        );

        assertThat(corp.getAccountType()).isEqualTo(AccountType.CORPORATE);
        assertThat(corp.getAvailableFunds()).isEqualByComparingTo("6000.00");

        // Withdraw $2000. 1% fee = $20. Total required = $2020.
        // Balance becomes 1000 - 2020 = -1020.00
        corp.withdraw(new BigDecimal("2000.00"));
        assertThat(corp.getBalance()).isEqualByComparingTo("-1020.00");
        assertThat(corp.getAvailableFunds()).isEqualByComparingTo("3980.00");
    }

    @Test
    @DisplayName("CorporateAccount rejects withdrawal exceeding combined balance and credit line")
    void testCorporateAccountExceedingCreditLimit() {
        CorporateAccount corp = new CorporateAccount(
                "Small Business Inc",
                "ops@smallbiz.com",
                new BigDecimal("200.00"),
                new BigDecimal("800.00"), // Total available = $1000.00
                "TAX-112233"
        );

        assertThrows(
                CreditLimitExceededException.class,
                () -> corp.withdraw(new BigDecimal("1200.00"))
        );
    }

    @Test
    @DisplayName("Validation prevents negative deposit or invalid email")
    void testAccountValidation() {
        assertThrows(ValidationException.class, () -> new CustomerAccount(
                "Invalid", "invalid-email-no-at", BigDecimal.TEN, CustomerTier.STANDARD));

        CustomerAccount account = new CustomerAccount("Valid", "valid@mail.com", BigDecimal.TEN, CustomerTier.STANDARD);
        assertThrows(ValidationException.class, () -> account.deposit(new BigDecimal("-50.00")));
    }
}
