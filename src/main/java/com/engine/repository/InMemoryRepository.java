package com.engine.repository;

import com.engine.exception.ValidationException;
import com.engine.model.BaseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Generic in-memory repository implementation utilizing ConcurrentMap and Set collections
 * for thread-safe storage, fast O(1) key lookups, and unique entity tracking.
 *
 * @param <T>  Entity type extending BaseEntity
 * @param <ID> Unique identifier type
 */
public class InMemoryRepository<T extends BaseEntity, ID> implements Repository<T, ID> {

    protected final ConcurrentMap<ID, T> storage = new ConcurrentHashMap<>();
    protected final Set<T> entitySet = ConcurrentHashMap.newKeySet();

    @SuppressWarnings("unchecked")
    @Override
    public T save(T entity) {
        if (entity == null) {
            throw new ValidationException("Cannot persist null entity");
        }
        entity.validate();
        ID id = (ID) entity.getId();

        // If replacing an existing instance with same id, remove old reference from entitySet
        T existing = storage.get(id);
        if (existing != null) {
            entitySet.remove(existing);
        }

        storage.put(id, entity);
        entitySet.add(entity);
        return entity;
    }

    @Override
    public Optional<T> findById(ID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<T> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(storage.values()));
    }

    /**
     * Returns an unmodifiable snapshot of the underlying unique Set collection.
     */
    public Set<T> asSet() {
        return Collections.unmodifiableSet(entitySet);
    }

    @Override
    public boolean deleteById(ID id) {
        if (id == null) {
            return false;
        }
        T removed = storage.remove(id);
        if (removed != null) {
            entitySet.remove(removed);
            return true;
        }
        return false;
    }

    @Override
    public boolean existsById(ID id) {
        return id != null && storage.containsKey(id);
    }

    @Override
    public long count() {
        return storage.size();
    }

    @Override
    public void clear() {
        storage.clear();
        entitySet.clear();
    }
}
