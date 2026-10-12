package com.engine.exception;

import java.util.Collections;
import java.util.Map;

/**
 * Unchecked exception thrown when domain model validation fails.
 * Collects one or more field violation details.
 */
public class ValidationException extends BusinessEngineRuntimeException {
    private final Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_FAILED, message);
        this.fieldErrors = Collections.emptyMap();
    }

    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(ErrorCode.VALIDATION_FAILED, message);
        this.fieldErrors = fieldErrors != null ? Collections.unmodifiableMap(fieldErrors) : Collections.emptyMap();
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    @Override
    public String getMessage() {
        if (fieldErrors.isEmpty()) {
            return super.getMessage();
        }
        return super.getMessage() + " Errors: " + fieldErrors;
    }
}
