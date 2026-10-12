package com.engine.service;

import com.engine.exception.CreditLimitExceededException;
import com.engine.exception.EntityNotFoundException;
import com.engine.exception.InsufficientInventoryException;
import com.engine.exception.InvalidOrderStateException;
import com.engine.model.CustomerAccount;
import com.engine.model.CustomerTier;
import com.engine.model.Dimensions;
import com.engine.model.Order;
import com.engine.model.OrderStatus;
import com.engine.model.PhysicalProduct;
import com.engine.repository.AccountRepository;
import com.engine.repository.OrderRepository;
import com.engine.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Order Processing Engine Integration Tests")
class OrderProcessingEngineTest {

    private ProductRepository productRepository;
    private AccountRepository accountRepository;
    private OrderRepository orderRepository;
    private OrderProcessingEngine engine;

    @BeforeEach
    void setUp() {
        productRepository = new ProductRepository();
        accountRepository = new AccountRepository();
        orderRepository = new OrderRepository();
        engine = new OrderProcessingEngine(productRepository, accountRepository, orderRepository);
    }

    @Test
    @DisplayName("Complete checkout workflow deducts balance, applies tier discount, and reduces inventory")
    void testSuccessfulCheckout() throws Exception {
        // Customer with Gold tier (10% discount)
        CustomerAccount customer = new CustomerAccount(
                "Alice", "alice@test.com", new BigDecimal("500.00"), CustomerTier.GOLD);
        accountRepository.save(customer);

        // Product with 2kg weight -> shipping = $5.00, base price = $100.00 -> final price = $105.00
        PhysicalProduct product = new PhysicalProduct(
                "SKU-CHAIR", "Office Chair", new BigDecimal("100.00"), 10, 2.0,
                new Dimensions(10, 10, 10), false);
        productRepository.save(product);

        Order order = engine.createOrder(customer.getId());
        engine.addItemToOrder(order.getId(), product.getSku(), 2);

        // Subtotal = 2 * 105.00 = 210.00
        // Gold discount 10% = 21.00
        // Final total = 189.00
        Order processed = engine.processCheckout(order.getId());

        assertThat(processed.getStatus()).isEqualTo(OrderStatus.APPROVED);
        assertThat(processed.getDiscountAmount()).isEqualByComparingTo("21.00");
        assertThat(processed.getTotalAmount()).isEqualByComparingTo("189.00");

        // Check account balance: 500 - 189 = 311.00
        CustomerAccount updatedAccount = (CustomerAccount) accountRepository.findById(customer.getId()).orElseThrow();
        assertThat(updatedAccount.getBalance()).isEqualByComparingTo("311.00");

        // Reward points (Gold multiplier 1.5 * 189.00 = 283.50)
        assertThat(updatedAccount.getRewardPoints()).isEqualByComparingTo("283.50");

        // Stock quantity: 10 - 2 = 8
        PhysicalProduct updatedProduct = (PhysicalProduct) productRepository.findBySku("SKU-CHAIR").orElseThrow();
        assertThat(updatedProduct.getStockQuantity()).isEqualTo(8);
    }

    @Test
    @DisplayName("Checkout fails with CreditLimitExceededException when account has insufficient funds")
    void testCheckoutInsufficientFunds() {
        CustomerAccount customer = new CustomerAccount(
                "Bob", "bob@test.com", new BigDecimal("50.00"), CustomerTier.STANDARD);
        accountRepository.save(customer);

        PhysicalProduct product = new PhysicalProduct(
                "SKU-PHONE", "Smartphone", new BigDecimal("800.00"), 5, 0.5,
                new Dimensions(5, 5, 1), false);
        productRepository.save(product);

        Order order = engine.createOrder(customer.getId());
        engine.addItemToOrder(order.getId(), product.getSku(), 1);

        assertThrows(
                CreditLimitExceededException.class,
                () -> engine.processCheckout(order.getId())
        );

        // Ensure order remains PENDING and stock is untouched
        Order currentOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(currentOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(productRepository.findBySku("SKU-PHONE").orElseThrow().getStockQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Checkout fails with InsufficientInventoryException when item stock is depleted")
    void testCheckoutInsufficientStock() throws Exception {
        CustomerAccount customer = new CustomerAccount(
                "Charlie", "charlie@test.com", new BigDecimal("1000.00"), CustomerTier.STANDARD);
        accountRepository.save(customer);

        PhysicalProduct product = new PhysicalProduct(
                "SKU-GPU", "Graphics Card", new BigDecimal("500.00"), 2, 1.0,
                new Dimensions(10, 5, 2), false);
        productRepository.save(product);

        Order order = engine.createOrder(customer.getId());
        engine.addItemToOrder(order.getId(), product.getSku(), 2);

        // Another actor purchases 1 unit before Charlie checks out
        product.reduceStock(1);
        productRepository.save(product);

        assertThrows(
                InsufficientInventoryException.class,
                () -> engine.processCheckout(order.getId())
        );
    }

    @Test
    @DisplayName("Illegal state transitions throw checked InvalidOrderStateException")
    void testIllegalStateTransitions() throws Exception {
        CustomerAccount customer = new CustomerAccount(
                "David", "david@test.com", new BigDecimal("1000.00"), CustomerTier.STANDARD);
        accountRepository.save(customer);

        PhysicalProduct product = new PhysicalProduct(
                "SKU-PEN", "Stylus", new BigDecimal("20.00"), 50, 0.1,
                new Dimensions(1, 1, 1), false);
        productRepository.save(product);

        Order order = engine.createOrder(customer.getId());
        engine.addItemToOrder(order.getId(), product.getSku(), 1);

        // Attempting to deliver an unapproved order
        assertThrows(
                InvalidOrderStateException.class,
                () -> engine.deliverOrder(order.getId())
        );

        // Normal flow
        engine.processCheckout(order.getId());
        engine.shipOrder(order.getId());
        engine.deliverOrder(order.getId());

        // Cannot cancel already delivered order
        assertThrows(
                InvalidOrderStateException.class,
                () -> engine.cancelOrder(order.getId())
        );
    }

    @Test
    @DisplayName("Cancelling an approved order restores inventory and refunds account balance")
    void testCancelApprovedOrderRollback() throws Exception {
        CustomerAccount customer = new CustomerAccount(
                "Eve", "eve@test.com", new BigDecimal("200.00"), CustomerTier.STANDARD);
        accountRepository.save(customer);

        PhysicalProduct product = new PhysicalProduct(
                "SKU-BOOK", "Core Java Guide", new BigDecimal("40.00"), 10, 0.5,
                new Dimensions(5, 5, 1), false);
        productRepository.save(product);

        Order order = engine.createOrder(customer.getId());
        engine.addItemToOrder(order.getId(), product.getSku(), 2);

        engine.processCheckout(order.getId());
        assertThat(productRepository.findBySku("SKU-BOOK").orElseThrow().getStockQuantity()).isEqualTo(8);

        // Cancel the order
        engine.cancelOrder(order.getId());

        // Verify status, balance refund, and inventory replenishment
        Order cancelledOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);

        CustomerAccount refundedAccount = (CustomerAccount) accountRepository.findById(customer.getId()).orElseThrow();
        assertThat(refundedAccount.getBalance()).isEqualByComparingTo("200.00");

        PhysicalProduct restockedProduct = (PhysicalProduct) productRepository.findBySku("SKU-BOOK").orElseThrow();
        assertThat(restockedProduct.getStockQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("Creating order for non-existent account throws EntityNotFoundException")
    void testUnknownAccountThrowsEntityNotFoundException() {
        assertThrows(
                EntityNotFoundException.class,
                () -> engine.createOrder("NON-EXISTENT-ID")
        );
    }
}
