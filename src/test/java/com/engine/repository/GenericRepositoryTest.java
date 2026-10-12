package com.engine.repository;

import com.engine.exception.DuplicateEntityException;
import com.engine.model.CustomerAccount;
import com.engine.model.CustomerTier;
import com.engine.model.Dimensions;
import com.engine.model.PhysicalProduct;
import com.engine.model.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Generic Collections & Repository Tests")
class GenericRepositoryTest {

    private ProductRepository productRepository;
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        productRepository = new ProductRepository();
        accountRepository = new AccountRepository();
    }

    @Test
    @DisplayName("Generic repository supports CRUD and maintains consistent Set views")
    void testGenericCrudOperations() {
        PhysicalProduct p1 = new PhysicalProduct("SKU-1", "Prod 1", BigDecimal.TEN, 5, 1.0, new Dimensions(10, 10, 10), false);
        PhysicalProduct p2 = new PhysicalProduct("SKU-2", "Prod 2", BigDecimal.valueOf(20), 15, 2.0, new Dimensions(20, 20, 20), false);

        productRepository.save(p1);
        productRepository.save(p2);

        assertThat(productRepository.count()).isEqualTo(2);
        assertThat(productRepository.existsById(p1.getId())).isTrue();
        assertThat(productRepository.findById(p1.getId())).contains(p1);
        assertThat(productRepository.asSet()).containsExactlyInAnyOrder(p1, p2);

        boolean deleted = productRepository.deleteById(p1.getId());
        assertThat(deleted).isTrue();
        assertThat(productRepository.count()).isEqualTo(1);
        assertThat(productRepository.findById(p1.getId())).isEmpty();
        assertThat(productRepository.asSet()).containsExactly(p2);
    }

    @Test
    @DisplayName("ProductRepository enforces unique SKU constraint with DuplicateEntityException")
    void testDuplicateSkuThrowsException() {
        PhysicalProduct p1 = new PhysicalProduct("SKU-DUP", "Mouse", BigDecimal.TEN, 5, 0.5, new Dimensions(5, 5, 5), false);
        PhysicalProduct p2 = new PhysicalProduct("SKU-DUP", "Gaming Mouse", BigDecimal.valueOf(30), 10, 0.8, new Dimensions(5, 5, 5), false);

        productRepository.save(p1);

        DuplicateEntityException ex = assertThrows(
                DuplicateEntityException.class,
                () -> productRepository.save(p2)
        );

        assertThat(ex.getEntityType()).isEqualTo("Product");
        assertThat(ex.getFieldName()).isEqualTo("SKU");
        assertThat(ex.getDuplicateValue()).isEqualTo("SKU-DUP");
    }

    @Test
    @DisplayName("AccountRepository enforces unique email constraint across case insensitivity")
    void testDuplicateEmailThrowsException() {
        CustomerAccount acc1 = new CustomerAccount("Alice", "alice@example.com", BigDecimal.TEN, CustomerTier.STANDARD);
        CustomerAccount acc2 = new CustomerAccount("Alice Two", "ALICE@EXAMPLE.COM", BigDecimal.valueOf(50), CustomerTier.SILVER);

        accountRepository.save(acc1);

        DuplicateEntityException ex = assertThrows(
                DuplicateEntityException.class,
                () -> accountRepository.save(acc2)
        );

        assertThat(ex.getEntityType()).isEqualTo("Account");
        assertThat(ex.getFieldName()).isEqualTo("email");
    }

    @Test
    @DisplayName("ProductRepository queries low-stock items correctly")
    void testFindLowStock() {
        PhysicalProduct highStock = new PhysicalProduct("SKU-HIGH", "Item A", BigDecimal.ONE, 100, 1.0, new Dimensions(1, 1, 1), false);
        PhysicalProduct lowStock = new PhysicalProduct("SKU-LOW", "Item B", BigDecimal.ONE, 2, 1.0, new Dimensions(1, 1, 1), false);

        productRepository.save(highStock);
        productRepository.save(lowStock);

        List<Product> results = productRepository.findLowStock(5);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSku()).isEqualTo("SKU-LOW");
    }
}
