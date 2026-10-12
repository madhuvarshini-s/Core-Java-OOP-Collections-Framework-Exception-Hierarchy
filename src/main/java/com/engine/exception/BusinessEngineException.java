package com.engine.exception;

import java.time.Instant;

/**
 * Root checked exception for business domain workflows.
 * Forces calling layers to explicitly handle recoverable business constraint violations.
 */
public class BusinessEngineException extends Exception {
    private final ErrorCode errorCode;
    private final Instant timestamp;

    public BusinessEngineException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.timestamp = Instant.now();
    }

    public BusinessEngineException(ErrorCode errorCode, String message, Throwable cause) {
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
        return "BusinessEngineException{" +
                "errorCode=" + errorCode +
                ", timestamp=" + timestamp +
                ", message=" + getMessage() +
                '}';
    }
}
