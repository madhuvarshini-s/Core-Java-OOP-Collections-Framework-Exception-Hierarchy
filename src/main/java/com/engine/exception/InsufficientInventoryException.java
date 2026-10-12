package com.engine.exception;

/**
 * Checked exception thrown when an order or reservation requests more product units
 * than are currently available in inventory.
 */
public class InsufficientInventoryException extends BusinessEngineException {
    private final String productSku;
    private final int requestedQuantity;
    private final int availableQuantity;

    public InsufficientInventoryException(String productSku, int requestedQuantity, int availableQuantity) {
        super(ErrorCode.INSUFFICIENT_INVENTORY,
                String.format("Insufficient stock for SKU '%s': requested %d, available %d",
                        productSku, requestedQuantity, availableQuantity));
        this.productSku = productSku;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
    }

    public String getProductSku() {
        return productSku;
    }

    public int getRequestedQuantity() {
        return requestedQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
