package com.engine.repository;

import com.engine.model.Order;
import com.engine.model.OrderStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Specialized repository for Order aggregates supporting relational lookups
 * and status grouping with the Java Stream API.
 */
public class OrderRepository extends InMemoryRepository<Order, String> {

    public List<Order> findByAccountId(String accountId) {
        if (accountId == null) {
            return List.of();
        }
        return storage.values().stream()
                .filter(order -> order.getAccountId().equals(accountId))
                .toList();
    }

    public List<Order> findByStatus(OrderStatus status) {
        if (status == null) {
            return List.of();
        }
        return storage.values().stream()
                .filter(order -> order.getStatus() == status)
                .toList();
    }

    /**
     * Groups orders by status using Collectors.groupingBy.
     */
    public Map<OrderStatus, List<Order>> groupByStatus() {
        return storage.values().stream()
                .collect(Collectors.groupingBy(Order::getStatus));
    }
}
