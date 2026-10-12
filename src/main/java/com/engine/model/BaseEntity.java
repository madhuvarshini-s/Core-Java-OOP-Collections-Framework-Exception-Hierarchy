package com.engine.model;

import com.engine.exception.ValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Abstract root entity encapsulating identity, audit timestamps,
 * equality semantics, and lifecycle validation.
 */
public abstract class BaseEntity {
    private final String id;
    private final Instant createdAt;
    private Instant updatedAt;

    protected BaseEntity() {
        this(UUID.randomUUID().toString());
    }

    protected BaseEntity(String id) {
        if (id == null || id.isBlank()) {
            throw new ValidationException("Entity ID cannot be null or blank");
        }
        this.id = id.trim();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public String getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    protected void touch() {
        this.updatedAt = Instant.now();
    }

    /**
     * Validates domain constraints specific to the concrete subclass.
     * Throws ValidationException if invariants are violated.
     */
    public abstract void validate();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseEntity that = (BaseEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "id='" + id + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
