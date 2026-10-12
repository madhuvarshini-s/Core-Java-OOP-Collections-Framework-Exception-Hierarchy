package com.engine.service;

import com.engine.model.CustomerAccount;
import com.engine.model.CustomerTier;
import com.engine.model.Dimensions;
import com.engine.model.InventoryReport;
import com.engine.model.Order;
import com.engine.model.OrderItem;
import com.engine.model.OrderSummary;
import com.engine.model.OrderStatus;
import com.engine.model.PhysicalProduct;
import com.engine.model.Product;
import com.engine.repository.AccountRepository;
import com.engine.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Analytics Engine & Stream Processing Tests")
class AnalyticsEngineTest {

    private AnalyticsEngine analyticsEngine;
    private AccountRepository accountRepository;
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        analyticsEngine = new AnalyticsEngine();
        accountRepository = new AccountRepository();
        productRepository = new ProductRepository();
    }

    @Test
    @DisplayName("Streams sort orders by total amount descending")
    void testSortOrdersByAmount() {
        Order o1 = new Order("ACC-1");
        o1.addItem(new OrderItem("SKU-1", "Item 1", 1, new BigDecimal("50.00")));

        Order o2 = new Order("ACC-2");
        o2.addItem(new OrderItem("SKU-2", "Item 2", 1, new BigDecimal("200.00")));

        Order o3 = new Order("ACC-3");
        o3.addItem(new OrderItem("SKU-3", "Item 3", 1, new BigDecimal("100.00")));

        List<Order> sorted = analyticsEngine.sortOrdersByAmount(List.of(o1, o2, o3), true);

        assertThat(sorted).containsExactly(o2, o3, o1);
    }

    @Test
    @DisplayName("Streams filter orders by arbitrary predicate lambdas")
    void testFilterOrders() throws Exception {
        Order o1 = new Order("ACC-1");
        o1.addItem(new OrderItem("SKU-1", "Item 1", 1, new BigDecimal("50.00")));
        o1.approve();

        Order o2 = new Order("ACC-2");
        o2.addItem(new OrderItem("SKU-2", "Item 2", 1, new BigDecimal("150.00")));

        List<Order> approvedOrders = analyticsEngine.filterOrders(
                List.of(o1, o2),
                order -> order.getStatus() == OrderStatus.APPROVED
        );

        assertThat(approvedOrders).containsExactly(o1);
    }

    @Test
    @DisplayName("Calculates total revenue and average order value accurately")
    void testRevenueAndAov() {
        Order o1 = new Order("ACC-1");
        o1.addItem(new OrderItem("SKU-1", "Item 1", 1, new BigDecimal("100.00")));

        Order o2 = new Order("ACC-2");
        o2.addItem(new OrderItem("SKU-2", "Item 2", 1, new BigDecimal("300.00")));

        List<Order> orders = List.of(o1, o2);

        BigDecimal revenue = analyticsEngine.calculateTotalRevenue(orders);
        BigDecimal aov = analyticsEngine.calculateAverageOrderValue(orders);

        assertThat(revenue).isEqualByComparingTo("400.00");
        assertThat(aov).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("Groups revenue by Customer Tier using Stream Collectors")
    void testGroupRevenueByTier() {
        CustomerAccount silverUser = new CustomerAccount("Silver User", "silver@example.com", BigDecimal.ZERO, CustomerTier.SILVER);
        CustomerAccount goldUser = new CustomerAccount("Gold User", "gold@example.com", BigDecimal.ZERO, CustomerTier.GOLD);
        accountRepository.save(silverUser);
        accountRepository.save(goldUser);

        Order o1 = new Order(silverUser.getId());
        o1.addItem(new OrderItem("SKU-1", "Item 1", 1, new BigDecimal("100.00")));

        Order o2 = new Order(goldUser.getId());
        o2.addItem(new OrderItem("SKU-2", "Item 2", 1, new BigDecimal("250.00")));

        Order o3 = new Order(goldUser.getId());
        o3.addItem(new OrderItem("SKU-3", "Item 3", 1, new BigDecimal("150.00")));

        Map<CustomerTier, BigDecimal> revenueMap = analyticsEngine.groupRevenueByCustomerTier(
                List.of(o1, o2, o3), accountRepository);

        assertThat(revenueMap.get(CustomerTier.SILVER)).isEqualByComparingTo("100.00");
        assertThat(revenueMap.get(CustomerTier.GOLD)).isEqualByComparingTo("400.00");
    }

    @Test
    @DisplayName("Finds top-selling products by total quantity ordered")
    void testFindTopSellingProducts() {
        Order o1 = new Order("ACC-1");
        o1.addItem(new OrderItem("SKU-LAPTOP", "Laptop", 2, new BigDecimal("1000.00")));
        o1.addItem(new OrderItem("SKU-MOUSE", "Mouse", 5, new BigDecimal("25.00")));

        Order o2 = new Order("ACC-2");
        o2.addItem(new OrderItem("SKU-MOUSE", "Mouse", 10, new BigDecimal("25.00")));
        o2.addItem(new OrderItem("SKU-KEYBOARD", "Keyboard", 1, new BigDecimal("80.00")));

        List<Map.Entry<String, Integer>> topProducts = analyticsEngine.findTopSellingProducts(List.of(o1, o2), 2);

        assertThat(topProducts).hasSize(2);
        assertThat(topProducts.get(0).getKey()).isEqualTo("SKU-MOUSE");
        assertThat(topProducts.get(0).getValue()).isEqualTo(15);
        assertThat(topProducts.get(1).getKey()).isEqualTo("SKU-LAPTOP");
        assertThat(topProducts.get(1).getValue()).isEqualTo(2);
    }

    @Test
    @DisplayName("Generates comprehensive inventory health and valuation report")
    void testGenerateInventoryReport() {
        PhysicalProduct p1 = new PhysicalProduct("SKU-1", "Item 1", new BigDecimal("50.00"), 2, 1.0, new Dimensions(1, 1, 1), false);
        PhysicalProduct p2 = new PhysicalProduct("SKU-2", "Item 2", new BigDecimal("100.00"), 10, 1.0, new Dimensions(1, 1, 1), false);
        productRepository.save(p1);
        productRepository.save(p2);

        InventoryReport report = analyticsEngine.generateInventoryReport(productRepository, 3);

        assertThat(report.totalProducts()).isEqualTo(2);
        assertThat(report.lowStockCount()).isEqualTo(1); // p1 has 2 units (<= 3)
        // Asset value: (50*2) + (100*10) = 100 + 1000 = 1100.00
        assertThat(report.totalCatalogValue()).isEqualByComparingTo("1100.00");
    }
}
