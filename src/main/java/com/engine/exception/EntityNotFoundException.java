package com.engine.exception;

/**
 * Unchecked exception thrown when an entity is not found in the repository or storage.
 */
public class EntityNotFoundException extends BusinessEngineRuntimeException {
    private final String entityType;
    private final String identifier;

    public EntityNotFoundException(String entityType, String identifier) {
        super(ErrorCode.ENTITY_NOT_FOUND,
                String.format("%s with identifier '%s' was not found", entityType, identifier));
        this.entityType = entityType;
        this.identifier = identifier;
    }

    public String getEntityType() {
        return entityType;
    }

    public String getIdentifier() {
        return identifier;
    }
}
