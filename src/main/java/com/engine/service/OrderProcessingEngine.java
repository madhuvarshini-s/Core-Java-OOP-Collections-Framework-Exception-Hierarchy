package com.engine.service;

import com.engine.exception.BusinessEngineException;
import com.engine.exception.CreditLimitExceededException;
import com.engine.exception.EntityNotFoundException;
import com.engine.exception.InsufficientInventoryException;
import com.engine.exception.InvalidOrderStateException;
import com.engine.exception.ValidationException;
import com.engine.model.Account;
import com.engine.model.CustomerAccount;
import com.engine.model.Order;
import com.engine.model.OrderItem;
import com.engine.model.OrderStatus;
import com.engine.model.Product;
import com.engine.repository.AccountRepository;
import com.engine.repository.OrderRepository;
import com.engine.repository.ProductRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Core transactional service coordinating order lifecycle, inventory reservations,
 * financial settlements, and custom checked/unchecked business exception handling.
 */
public class OrderProcessingEngine {

    private final ProductRepository productRepository;
    private final AccountRepository accountRepository;
    private final OrderRepository orderRepository;

    public OrderProcessingEngine(ProductRepository productRepository,
                                 AccountRepository accountRepository,
                                 OrderRepository orderRepository) {
        this.productRepository = Objects.requireNonNull(productRepository, "ProductRepository cannot be null");
        this.accountRepository = Objects.requireNonNull(accountRepository, "AccountRepository cannot be null");
        this.orderRepository = Objects.requireNonNull(orderRepository, "OrderRepository cannot be null");
    }

    /**
     * Initializes a new pending order for the specified account.
     */
    public Order createOrder(String accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new EntityNotFoundException("Account", accountId);
        }
        Order order = new Order(accountId);
        return orderRepository.save(order);
    }

    /**
     * Appends an item line to a pending order after verifying product existence and stock availability.
     */
    public synchronized Order addItemToOrder(String orderId, String sku, int quantity) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new EntityNotFoundException("Product (SKU)", sku));

        if (!product.isAvailable(quantity)) {
            throw new ValidationException(String.format(
                    "Product '%s' has insufficient stock (%d) for requested quantity (%d)",
                    sku, product.getStockQuantity(), quantity));
        }

        BigDecimal unitPrice = product.calculateFinalPrice();
        OrderItem item = new OrderItem(product.getSku(), product.getName(), quantity, unitPrice);
        order.addItem(item);

        return orderRepository.save(order);
    }

    /**
     * Executes order checkout:
     * 1. Validates order status is PENDING.
     * 2. Checks inventory for all items.
     * 3. Calculates customer tier discounts.
     * 4. Deducts funds from account (throws checked CreditLimitExceededException).
     * 5. Deducts inventory for each item (throws checked InsufficientInventoryException).
     * 6. Grants reward points for customer accounts.
     * 7. Approves and transitions order state.
     */
    public synchronized Order processCheckout(String orderId) throws BusinessEngineException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(orderId, order.getStatus().name(), OrderStatus.APPROVED.name());
        }

        if (order.getItems().isEmpty()) {
            throw new ValidationException("Cannot checkout an empty order");
        }

        Account account = accountRepository.findById(order.getAccountId())
                .orElseThrow(() -> new EntityNotFoundException("Account", order.getAccountId()));

        // Pre-check stock for all line items before making modifications
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findBySku(item.productSku())
                    .orElseThrow(() -> new EntityNotFoundException("Product (SKU)", item.productSku()));
            if (product.getStockQuantity() < item.quantity()) {
                throw new InsufficientInventoryException(item.productSku(), item.quantity(), product.getStockQuantity());
            }
        }

        // Apply customer tier discount if account is a retail customer
        if (account instanceof CustomerAccount customerAccount) {
            BigDecimal rawTotal = order.getItems().stream()
                    .map(OrderItem::subtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal discount = rawTotal.multiply(customerAccount.getTier().getDiscountRate())
                    .setScale(2, RoundingMode.HALF_UP);
            order.setDiscountAmount(discount);
        }

        BigDecimal finalAmount = order.getTotalAmount();

        // 1. Withdraw funds / credit from account (Checked: CreditLimitExceededException)
        account.withdraw(finalAmount);

        // 2. Deduct inventory (Checked: InsufficientInventoryException)
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findBySku(item.productSku()).orElseThrow();
            product.reduceStock(item.quantity());
            productRepository.save(product);
        }

        // 3. Award reward points if CustomerAccount
        if (account instanceof CustomerAccount customerAccount) {
            customerAccount.awardPoints(finalAmount);
        }

        // 4. Transition order status
        order.approve();

        // Persist updated states
        accountRepository.save(account);
        return orderRepository.save(order);
    }

    /**
     * Ships an approved order.
     */
    public synchronized Order shipOrder(String orderId) throws InvalidOrderStateException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
        order.ship();
        return orderRepository.save(order);
    }

    /**
     * Delivers a shipped order.
     */
    public synchronized Order deliverOrder(String orderId) throws InvalidOrderStateException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
        order.deliver();
        return orderRepository.save(order);
    }

    /**
     * Cancels an order and replenishes inventory / refunds account if already approved.
     */
    public synchronized Order cancelOrder(String orderId) throws InvalidOrderStateException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));

        OrderStatus currentStatus = order.getStatus();
        order.cancel();

        // If order had already been approved, rollback inventory and refund account
        if (currentStatus == OrderStatus.APPROVED) {
            Account account = accountRepository.findById(order.getAccountId())
                    .orElseThrow(() -> new EntityNotFoundException("Account", order.getAccountId()));
            account.deposit(order.getTotalAmount());
            accountRepository.save(account);

            for (OrderItem item : order.getItems()) {
                productRepository.findBySku(item.productSku()).ifPresent(p -> {
                    p.restock(item.quantity());
                    productRepository.save(p);
                });
            }
        }

        return orderRepository.save(order);
    }
}
