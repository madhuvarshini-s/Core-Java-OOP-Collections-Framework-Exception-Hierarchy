package com.engine.model;

/**
 * Lifecycle states of an Order within the processing pipeline.
 */
public enum OrderStatus {
    PENDING,
    APPROVED,
    SHIPPED,
    DELIVERED,
    CANCELLED
}
