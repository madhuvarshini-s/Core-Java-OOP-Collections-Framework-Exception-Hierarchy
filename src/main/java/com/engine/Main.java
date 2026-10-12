package com.engine;

import com.engine.exception.BusinessEngineException;
import com.engine.model.AccountType;
import com.engine.model.CustomerAccount;
import com.engine.model.CustomerTier;
import com.engine.model.DigitalProduct;
import com.engine.model.Dimensions;
import com.engine.model.InventoryReport;
import com.engine.model.Order;
import com.engine.model.OrderItem;
import com.engine.model.PhysicalProduct;
import com.engine.model.Product;
import com.engine.repository.AccountRepository;
import com.engine.repository.OrderRepository;
import com.engine.repository.ProductRepository;
import com.engine.service.AnalyticsEngine;
import com.engine.service.OrderProcessingEngine;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Interactive demonstration entry point for the Core Java Business Management Engine.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("   Core Java Business Management Engine - Live Demonstration   ");
        System.out.println("===============================================================\n");

        // 1. Initialize Repositories and Services
        ProductRepository productRepo = new ProductRepository();
        AccountRepository accountRepo = new AccountRepository();
        OrderRepository orderRepo = new OrderRepository();

        AnalyticsEngine analyticsEngine = new AnalyticsEngine();
        OrderProcessingEngine orderEngine = new OrderProcessingEngine(productRepo, accountRepo, orderRepo);

        // 2. Seed Products (OOP Polymorphism: Physical & Digital)
        PhysicalProduct serverRack = new PhysicalProduct(
                "SKU-HW-001",
                "Enterprise 42U Server Rack",
                new BigDecimal("1200.00"),
                15,
                45.0,
                new Dimensions(60, 100, 200),
                false
        );
        productRepo.save(serverRack);

        DigitalProduct cloudLicense = new DigitalProduct(
                "SKU-SW-002",
                "Enterprise Security Suite (1-Yr)",
                new BigDecimal("499.00"),
                500,
                "https://secure.downloads.enterprise/suite-v4.tar.gz",
                1073741824L,
                "ENT-LIC-2026-X89A"
        );
        productRepo.save(cloudLicense);

        System.out.println("[CATALOG] Loaded products into generic repository:");
        for (Product p : productRepo.findAll()) {
            System.out.printf(" - [%s] %s | Base: $%s | Final Price: $%s | Stock: %d%n",
                    p.getProductType(), p.getName(), p.getBasePrice(), p.calculateFinalPrice(), p.getStockQuantity());
        }

        // 3. Seed Accounts
        CustomerAccount customer = new CustomerAccount(
                "Alice Johnson",
                "alice.johnson@example.com",
                new BigDecimal("2500.00"),
                CustomerTier.GOLD
        );
        accountRepo.save(customer);

        System.out.println("\n[ACCOUNT] Seeded Account:");
        System.out.printf(" - Holder: %s | Tier: %s | Initial Balance: $%s%n",
                customer.getAccountHolder(), customer.getTier(), customer.getBalance());

        // 4. Create and Process an Order
        try {
            System.out.println("\n[WORKFLOW] Creating Order & Adding Line Items...");
            Order order = orderEngine.createOrder(customer.getId());
            orderEngine.addItemToOrder(order.getId(), serverRack.getSku(), 1);
            orderEngine.addItemToOrder(order.getId(), cloudLicense.getSku(), 2);

            System.out.println(" - Subtotal before discount: $" + order.getItems().stream()
                    .map(OrderItem::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add));

            System.out.println("[WORKFLOW] Processing Checkout...");
            Order approvedOrder = orderEngine.processCheckout(order.getId());
            System.out.printf(" - Order %s APPROVED! Total Charged: $%s (Gold Discount: $%s)%n",
                    approvedOrder.getId(), approvedOrder.getTotalAmount(), approvedOrder.getDiscountAmount());

            CustomerAccount updatedCustomer = (CustomerAccount) accountRepo.findById(customer.getId()).orElseThrow();
            System.out.printf(" - Remaining Balance: $%s | Reward Points Earned: %s%n",
                    updatedCustomer.getBalance(), updatedCustomer.getRewardPoints());

            // Advance through lifecycle
            orderEngine.shipOrder(approvedOrder.getId());
            System.out.println(" - Order transitioned to SHIPPED.");
            orderEngine.deliverOrder(approvedOrder.getId());
            System.out.println(" - Order transitioned to DELIVERED.");

        } catch (BusinessEngineException e) {
            System.err.printf("[ERROR] Business constraint violation: %s (Code: %s)%n",
                    e.getMessage(), e.getErrorCode());
        }

        // 5. Executive Analytics via Java Streams
        System.out.println("\n[ANALYTICS] Java Stream API Reporting:");
        BigDecimal totalRevenue = analyticsEngine.calculateTotalRevenue(orderRepo.findAll());
        System.out.println(" - Total Revenue: $" + totalRevenue);

        InventoryReport invReport = analyticsEngine.generateInventoryReport(productRepo, 10);
        System.out.printf(" - Catalog Products: %d | Low Stock Items: %d | Total Asset Value: $%s%n",
                invReport.totalProducts(), invReport.lowStockCount(), invReport.totalCatalogValue());

        List<Map.Entry<String, Integer>> topProducts = analyticsEngine.findTopSellingProducts(orderRepo.findAll(), 5);
        System.out.println(" - Top Selling SKU Units: " + topProducts);

        System.out.println("\n===============================================================");
        System.out.println("   Engine execution completed successfully.                   ");
        System.out.println("===============================================================");
    }
}
