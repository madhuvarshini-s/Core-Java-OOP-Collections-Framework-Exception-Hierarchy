package com.engine.repository;

import com.engine.exception.DuplicateEntityException;
import com.engine.model.Product;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Specialized repository for Product entities with secondary SKU indexing
 * and business query methods.
 */
public class ProductRepository extends InMemoryRepository<Product, String> {

    private final ConcurrentMap<String, Product> skuIndex = new ConcurrentHashMap<>();

    @Override
    public synchronized Product save(Product product) {
        if (product == null) {
            return super.save(null);
        }

        Product existingBySku = skuIndex.get(product.getSku());
        if (existingBySku != null && !existingBySku.getId().equals(product.getId())) {
            throw new DuplicateEntityException("Product", "SKU", product.getSku());
        }

        Product saved = super.save(product);
        skuIndex.put(product.getSku(), saved);
        return saved;
    }

    public Optional<Product> findBySku(String sku) {
        if (sku == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(skuIndex.get(sku));
    }

    public List<Product> findLowStock(int threshold) {
        return storage.values().stream()
                .filter(p -> p.getStockQuantity() <= threshold)
                .toList();
    }

    public List<Product> findByType(String productType) {
        if (productType == null) {
            return List.of();
        }
        return storage.values().stream()
                .filter(p -> p.getProductType().equalsIgnoreCase(productType))
                .toList();
    }

    @Override
    public synchronized boolean deleteById(String id) {
        Optional<Product> existing = findById(id);
        if (existing.isPresent()) {
            skuIndex.remove(existing.get().getSku());
            return super.deleteById(id);
        }
        return false;
    }

    @Override
    public synchronized void clear() {
        super.clear();
        skuIndex.clear();
    }
}
