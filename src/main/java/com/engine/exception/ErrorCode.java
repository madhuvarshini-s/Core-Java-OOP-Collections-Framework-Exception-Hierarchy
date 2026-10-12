package com.engine.exception;

/**
 * Standard business error codes categorized by fault domain.
 */
public enum ErrorCode {
    // Inventory & Catalog (Checked)
    INSUFFICIENT_INVENTORY("ERR_INV_001", "Requested inventory quantity is not available"),
    
    // Financial & Credit (Checked)
    CREDIT_LIMIT_EXCEEDED("ERR_FIN_001", "Transaction exceeds available account balance or credit limit"),
    
    // Order Lifecycle (Checked)
    INVALID_ORDER_STATE("ERR_ORD_001", "Illegal state transition requested for order"),

    // Entity & Repository (Unchecked)
    ENTITY_NOT_FOUND("ERR_REPO_001", "Requested entity could not be located"),
    DUPLICATE_ENTITY("ERR_REPO_002", "Entity violates unique constraint"),
    
    // Domain Validation (Unchecked)
    VALIDATION_FAILED("ERR_VAL_001", "Domain object fails validation constraints"),
    INVALID_ARGUMENT("ERR_ARG_001", "Supplied argument is illegal or out of range");

    private final String code;
    private final String defaultMessage;

    ErrorCode(String code, String defaultMessage) {
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
