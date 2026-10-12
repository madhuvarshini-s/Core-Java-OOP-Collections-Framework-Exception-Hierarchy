package com.engine.model;

import com.engine.exception.InvalidOrderStateException;
import com.engine.exception.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Core transactional Aggregate representing a customer order.
 * Enforces strict encapsulation of order items and state transition invariants.
 */
public class Order extends BaseEntity {
    private final String accountId;
    private final List<OrderItem> items;
    private OrderStatus status;
    private BigDecimal discountAmount;

    public Order(String accountId) {
        super();
        if (accountId == null || accountId.isBlank()) {
            throw new ValidationException("Order accountId cannot be blank");
        }
        this.accountId = accountId;
        this.items = new ArrayList<>();
        this.status = OrderStatus.PENDING;
        this.discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public Order(String id, String accountId) {
        super(id);
        if (accountId == null || accountId.isBlank()) {
            throw new ValidationException("Order accountId cannot be blank");
        }
        this.accountId = accountId;
        this.items = new ArrayList<>();
        this.status = OrderStatus.PENDING;
        this.discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    public String getAccountId() {
        return accountId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        if (status != OrderStatus.PENDING) {
            throw new ValidationException("Discounts can only be modified on PENDING orders");
        }
        this.discountAmount = discountAmount != null ? discountAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        touch();
    }

    /**
     * Defensive copy of order items.
     */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Adds an item line to the order if the order is still PENDING.
     */
    public synchronized void addItem(OrderItem item) {
        Objects.requireNonNull(item, "OrderItem cannot be null");
        if (this.status != OrderStatus.PENDING) {
            throw new ValidationException("Cannot add items to order with status " + this.status);
        }
        this.items.add(item);
        touch();
    }

    /**
     * Calculates total sum of items minus discount.
     */
    public BigDecimal getTotalAmount() {
        BigDecimal subtotal = items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal total = subtotal.subtract(discountAmount);
        return total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : total.setScale(2, RoundingMode.HALF_UP);
    }

    // State transition methods with checked exceptions
    public synchronized void approve() throws InvalidOrderStateException {
        if (this.status != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(getId(), this.status.name(), OrderStatus.APPROVED.name());
        }
        if (this.items.isEmpty()) {
            throw new ValidationException("Cannot approve an order with zero items");
        }
        this.status = OrderStatus.APPROVED;
        touch();
    }

    public synchronized void ship() throws InvalidOrderStateException {
        if (this.status != OrderStatus.APPROVED) {
            throw new InvalidOrderStateException(getId(), this.status.name(), OrderStatus.SHIPPED.name());
        }
        this.status = OrderStatus.SHIPPED;
        touch();
    }

    public synchronized void deliver() throws InvalidOrderStateException {
        if (this.status != OrderStatus.SHIPPED) {
            throw new InvalidOrderStateException(getId(), this.status.name(), OrderStatus.DELIVERED.name());
        }
        this.status = OrderStatus.DELIVERED;
        touch();
    }

    public synchronized void cancel() throws InvalidOrderStateException {
        if (this.status == OrderStatus.SHIPPED || this.status == OrderStatus.DELIVERED) {
            throw new InvalidOrderStateException(getId(), this.status.name(), OrderStatus.CANCELLED.name());
        }
        this.status = OrderStatus.CANCELLED;
        touch();
    }

    /**
     * Produces an immutable summary snapshot.
     */
    public OrderSummary toSummary() {
        int totalQuantity = items.stream().mapToInt(OrderItem::quantity).sum();
        return new OrderSummary(getId(), accountId, getTotalAmount(), status, getCreatedAt(), totalQuantity);
    }

    @Override
    public void validate() {
        if (accountId == null || accountId.isBlank()) {
            throw new ValidationException("Order account ID cannot be blank");
        }
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + getId() + '\'' +
                ", accountId='" + accountId + '\'' +
                ", status=" + status +
                ", itemsCount=" + items.size() +
                ", totalAmount=" + getTotalAmount() +
                '}';
    }
}
