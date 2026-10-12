package com.engine.service;

import com.engine.model.Account;
import com.engine.model.CustomerAccount;
import com.engine.model.CustomerTier;
import com.engine.model.InventoryReport;
import com.engine.model.Order;
import com.engine.model.OrderItem;
import com.engine.model.OrderSummary;
import com.engine.model.Product;
import com.engine.repository.AccountRepository;
import com.engine.repository.ProductRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Functional business analytics engine demonstrating Java Streams,
 * lambdas, custom comparators, predicate chaining, and collector aggregations.
 */
public class AnalyticsEngine {

    /**
     * Filters orders using arbitrary predicate lambda expressions.
     */
    public List<Order> filterOrders(List<Order> orders, Predicate<Order> predicate) {
        Objects.requireNonNull(orders, "Orders list cannot be null");
        Objects.requireNonNull(predicate, "Predicate cannot be null");
        return orders.stream()
                .filter(predicate)
                .toList();
    }

    /**
     * Sorts orders by total monetary amount ascending or descending.
     */
    public List<Order> sortOrdersByAmount(List<Order> orders, boolean descending) {
        Objects.requireNonNull(orders, "Orders list cannot be null");
        Comparator<Order> comparator = Comparator.comparing(Order::getTotalAmount);
        if (descending) {
            comparator = comparator.reversed();
        }
        return orders.stream()
                .sorted(comparator.thenComparing(Order::getCreatedAt))
                .toList();
    }

    /**
     * Sorts products using a customizable comparator chain.
     */
    public List<Product> sortProducts(List<Product> products, Comparator<Product> comparator) {
        Objects.requireNonNull(products, "Products list cannot be null");
        Objects.requireNonNull(comparator, "Comparator cannot be null");
        return products.stream()
                .sorted(comparator)
                .toList();
    }

    /**
     * Calculates total revenue across all provided orders using BigDecimal reduce.
     */
    public BigDecimal calculateTotalRevenue(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Computes Average Order Value (AOV).
     */
    public BigDecimal calculateAverageOrderValue(List<Order> orders) {
        if (orders == null || orders.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal total = calculateTotalRevenue(orders);
        return total.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Finds orders exceeding a minimum monetary threshold and maps them to immutable summaries.
     */
    public List<OrderSummary> findOrdersAboveAmount(List<Order> orders, BigDecimal minAmount) {
        Objects.requireNonNull(orders, "Orders list cannot be null");
        Objects.requireNonNull(minAmount, "minAmount cannot be null");
        return orders.stream()
                .filter(order -> order.getTotalAmount().compareTo(minAmount) >= 0)
                .map(Order::toSummary)
                .toList();
    }

    /**
     * Groups total revenue by Customer Tier for customer accounts.
     */
    public Map<CustomerTier, BigDecimal> groupRevenueByCustomerTier(List<Order> orders, AccountRepository accountRepo) {
        Objects.requireNonNull(orders, "Orders list cannot be null");
        Objects.requireNonNull(accountRepo, "Account repository cannot be null");

        return orders.stream()
                .collect(Collectors.groupingBy(
                        order -> {
                            Optional<Account> accountOpt = accountRepo.findById(order.getAccountId());
                            if (accountOpt.isPresent() && accountOpt.get() instanceof CustomerAccount ca) {
                                return ca.getTier();
                            }
                            return CustomerTier.STANDARD;
                        },
                        Collectors.reducing(
                                BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                                Order::getTotalAmount,
                                BigDecimal::add
                        )
                ));
    }

    /**
     * Identifies top N best-selling products by units ordered across all orders.
     */
    public List<Map.Entry<String, Integer>> findTopSellingProducts(List<Order> orders, int topN) {
        Objects.requireNonNull(orders, "Orders list cannot be null");
        if (topN <= 0) {
            return List.of();
        }

        Map<String, Integer> productSales = orders.stream()
                .flatMap(order -> order.getItems().stream())
                .collect(Collectors.groupingBy(
                        OrderItem::productSku,
                        Collectors.summingInt(OrderItem::quantity)
                ));

        return productSales.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topN)
                .toList();
    }

    /**
     * Generates comprehensive inventory report aggregating product statistics.
     */
    public InventoryReport generateInventoryReport(ProductRepository productRepo, int lowStockThreshold) {
        Objects.requireNonNull(productRepo, "Product repository cannot be null");

        List<Product> allProducts = productRepo.findAll();
        int totalProducts = allProducts.size();

        int lowStockCount = (int) allProducts.stream()
                .filter(p -> p.getStockQuantity() <= lowStockThreshold)
                .count();

        BigDecimal totalAssetValue = allProducts.stream()
                .map(p -> p.getBasePrice().multiply(BigDecimal.valueOf(p.getStockQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new InventoryReport(totalProducts, lowStockCount, totalAssetValue);
    }
}
