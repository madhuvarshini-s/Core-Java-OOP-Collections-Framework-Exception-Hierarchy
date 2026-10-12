package com.engine.exception;

/**
 * Checked exception thrown when an illegal state transition is attempted
 * on an Order (e.g. attempting to cancel an already delivered or shipped order).
 */
public class InvalidOrderStateException extends BusinessEngineException {
    private final String orderId;
    private final String currentStatus;
    private final String targetStatus;

    public InvalidOrderStateException(String orderId, String currentStatus, String targetStatus) {
        super(ErrorCode.INVALID_ORDER_STATE,
                String.format("Invalid order state transition for order '%s': cannot transition from %s to %s",
                        orderId, currentStatus, targetStatus));
        this.orderId = orderId;
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getTargetStatus() {
        return targetStatus;
    }
}
