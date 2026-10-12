package com.engine.model;

import com.engine.exception.InsufficientInventoryException;
import com.engine.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Product Hierarchy & Encapsulation Tests")
class ProductHierarchyTest {

    @Test
    @DisplayName("PhysicalProduct correctly calculates shipping cost and polymorphic final price")
    void testPhysicalProductPricingAndShipping() {
        Dimensions dims = new Dimensions(10, 20, 30);
        PhysicalProduct product = new PhysicalProduct(
                "SKU-PHYS-100",
                "Ergonomic Chair",
                new BigDecimal("150.00"),
                10,
                4.0, // 4kg -> $10.00 shipping
                dims,
                true // +$5.00 fragile
        );

        assertThat(product.getProductType()).isEqualTo("PHYSICAL");
        assertThat(product.calculateShippingCost()).isEqualByComparingTo("15.00");
        assertThat(product.calculateFinalPrice()).isEqualByComparingTo("165.00");
        assertThat(product.getDimensions().volumeCc()).isEqualTo(6000.0);
    }

    @Test
    @DisplayName("DigitalProduct polymorphic pricing applies digital delivery rebate")
    void testDigitalProductPricing() {
        DigitalProduct digital = new DigitalProduct(
                "SKU-DIG-200",
                "Cloud IDE License",
                new BigDecimal("100.00"),
                999,
                "https://downloads.example.com/ide",
                524288000L,
                "LICENSE-KEY-ABC-123"
        );

        assertThat(digital.getProductType()).isEqualTo("DIGITAL");
        // 5% discount -> $95.00
        assertThat(digital.calculateFinalPrice()).isEqualByComparingTo("95.00");
        assertThat(digital.getLicenseKey()).isEqualTo("LICENSE-KEY-ABC-123");
    }

    @Test
    @DisplayName("Stock management reduces and restocks accurately")
    void testStockManagement() throws InsufficientInventoryException {
        PhysicalProduct product = new PhysicalProduct(
                "SKU-STOCK-1",
                "Mechanical Keyboard",
                new BigDecimal("80.00"),
                15,
                1.2,
                new Dimensions(40, 15, 4),
                false
        );

        assertThat(product.isAvailable(10)).isTrue();
        product.reduceStock(5);
        assertThat(product.getStockQuantity()).isEqualTo(10);

        product.restock(20);
        assertThat(product.getStockQuantity()).isEqualTo(30);
    }

    @Test
    @DisplayName("Reducing stock beyond availability throws InsufficientInventoryException")
    void testInsufficientInventoryThrowsCheckedException() {
        DigitalProduct product = new DigitalProduct(
                "SKU-DIG-LIMITED",
                "Limited Edition Asset",
                new BigDecimal("25.00"),
                3,
                "https://downloads.example.com/asset",
                1024L,
                "KEY-XYZ"
        );

        InsufficientInventoryException ex = assertThrows(
                InsufficientInventoryException.class,
                () -> product.reduceStock(5)
        );

        assertThat(ex.getProductSku()).isEqualTo("SKU-DIG-LIMITED");
        assertThat(ex.getRequestedQuantity()).isEqualTo(5);
        assertThat(ex.getAvailableQuantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("Validation prevents invalid entities with negative prices or invalid dimensions")
    void testProductValidationConstraints() {
        assertThrows(ValidationException.class, () -> new Dimensions(-1, 10, 10));

        assertThrows(ValidationException.class, () -> new PhysicalProduct(
                "", "Name", new BigDecimal("10.00"), 5, 2.0, new Dimensions(1, 1, 1), false));

        assertThrows(ValidationException.class, () -> new PhysicalProduct(
                "SKU-1", "Name", new BigDecimal("-10.00"), 5, 2.0, new Dimensions(1, 1, 1), false));
    }
}
