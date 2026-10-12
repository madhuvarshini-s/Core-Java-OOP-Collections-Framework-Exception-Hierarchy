package com.engine.repository;

import com.engine.model.BaseEntity;

import java.util.List;
import java.util.Optional;

/**
 * Generic repository interface providing standardized CRUD operations.
 *
 * @param <T>  the entity type extending BaseEntity
 * @param <ID> the unique identifier type
 */
public interface Repository<T extends BaseEntity, ID> {

    /**
     * Persists or updates the entity.
     *
     * @param entity the entity to save
     * @return the saved entity
     */
    T save(T entity);

    /**
     * Locates an entity by its primary identifier.
     *
     * @param id the entity identifier
     * @return Optional containing the entity if present
     */
    Optional<T> findById(ID id);

    /**
     * Returns an unmodifiable list of all persisted entities.
     *
     * @return list of entities
     */
    List<T> findAll();

    /**
     * Removes an entity by identifier.
     *
     * @param id the entity identifier
     * @return true if an entity was removed, false otherwise
     */
    boolean deleteById(ID id);

    /**
     * Checks if an entity exists for the given identifier.
     *
     * @param id the identifier to test
     * @return true if exists
     */
    boolean existsById(ID id);

    /**
     * Total count of persisted entities.
     *
     * @return count
     */
    long count();

    /**
     * Clears all stored entities.
     */
    void clear();
}
