package com.engine.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Exception Hierarchy & Error Contract Tests")
class ExceptionHierarchyTest {

    @Test
    @DisplayName("Checked exception hierarchy extends BusinessEngineException and carries typed codes")
    void testCheckedExceptionHierarchy() {
        InsufficientInventoryException invEx = new InsufficientInventoryException("SKU-123", 10, 4);
        CreditLimitExceededException credEx = new CreditLimitExceededException("ACC-99", new BigDecimal("500"), new BigDecimal("200"));
        InvalidOrderStateException stateEx = new InvalidOrderStateException("ORD-1", "PENDING", "DELIVERED");

        // Verify checked inheritance
        assertThat(invEx).isInstanceOf(BusinessEngineException.class);
        assertThat(invEx).isInstanceOf(Exception.class);
        assertThat(invEx).isNotInstanceOf(RuntimeException.class);

        assertThat(invEx.getErrorCode()).isEqualTo(ErrorCode.INSUFFICIENT_INVENTORY);
        assertThat(invEx.getProductSku()).isEqualTo("SKU-123");
        assertThat(invEx.getRequestedQuantity()).isEqualTo(10);
        assertThat(invEx.getAvailableQuantity()).isEqualTo(4);

        assertThat(credEx).isInstanceOf(BusinessEngineException.class);
        assertThat(credEx.getErrorCode()).isEqualTo(ErrorCode.CREDIT_LIMIT_EXCEEDED);
        assertThat(credEx.getAttemptedAmount()).isEqualByComparingTo("500");
        assertThat(credEx.getAvailableLimit()).isEqualByComparingTo("200");

        assertThat(stateEx).isInstanceOf(BusinessEngineException.class);
        assertThat(stateEx.getErrorCode()).isEqualTo(ErrorCode.INVALID_ORDER_STATE);
        assertThat(stateEx.getCurrentStatus()).isEqualTo("PENDING");
        assertThat(stateEx.getTargetStatus()).isEqualTo("DELIVERED");
    }

    @Test
    @DisplayName("Unchecked exception hierarchy extends BusinessEngineRuntimeException")
    void testUncheckedExceptionHierarchy() {
        EntityNotFoundException notFoundEx = new EntityNotFoundException("Order", "ORD-404");
        DuplicateEntityException dupEx = new DuplicateEntityException("Product", "SKU", "SKU-DUP");
        ValidationException valEx = new ValidationException("Form failed", Map.of("email", "Must be valid"));

        assertThat(notFoundEx).isInstanceOf(BusinessEngineRuntimeException.class);
        assertThat(notFoundEx).isInstanceOf(RuntimeException.class);
        assertThat(notFoundEx.getErrorCode()).isEqualTo(ErrorCode.ENTITY_NOT_FOUND);
        assertThat(notFoundEx.getEntityType()).isEqualTo("Order");
        assertThat(notFoundEx.getIdentifier()).isEqualTo("ORD-404");

        assertThat(dupEx).isInstanceOf(BusinessEngineRuntimeException.class);
        assertThat(dupEx.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_ENTITY);
        assertThat(dupEx.getFieldName()).isEqualTo("SKU");
        assertThat(dupEx.getDuplicateValue()).isEqualTo("SKU-DUP");

        assertThat(valEx).isInstanceOf(BusinessEngineRuntimeException.class);
        assertThat(valEx.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
        assertThat(valEx.getFieldErrors()).containsEntry("email", "Must be valid");
    }
}
