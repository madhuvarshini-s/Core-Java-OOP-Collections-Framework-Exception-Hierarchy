package com.engine.exception;

import java.time.Instant;

/**
 * Root unchecked (runtime) exception for business domain workflows.
 * Represents programming bugs, missing resources, or violated class invariants.
 */
public class BusinessEngineRuntimeException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Instant timestamp;

    public BusinessEngineRuntimeException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = Instant.now();
    }

    public BusinessEngineRuntimeException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.timestamp = Instant.now();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "BusinessEngineRuntimeException{" +
                "errorCode=" + errorCode +
                ", timestamp=" + timestamp +
                ", message=" + getMessage() +
                '}';
    }
}
