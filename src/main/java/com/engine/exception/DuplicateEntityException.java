package com.engine.exception;

/**
 * Unchecked exception thrown when an entity violates uniqueness constraints
 * (e.g., duplicate product SKU, duplicate email, duplicate account ID).
 */
public class DuplicateEntityException extends BusinessEngineRuntimeException {
    private final String entityType;
    private final String fieldName;
    private final Object duplicateValue;

    public DuplicateEntityException(String entityType, String fieldName, Object duplicateValue) {
        super(ErrorCode.DUPLICATE_ENTITY,
                String.format("%s already exists with %s = '%s'", entityType, fieldName, duplicateValue));
        this.entityType = entityType;
        this.fieldName = fieldName;
        this.duplicateValue = duplicateValue;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getDuplicateValue() {
        return duplicateValue;
    }
}
