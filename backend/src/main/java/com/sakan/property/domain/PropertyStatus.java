package com.sakan.property.domain;

import java.util.Set;

public enum PropertyStatus {
    DRAFT,
    PUBLISHED,
    RENTED,
    SOLD,
    ARCHIVED;

    public Set<PropertyStatus> allowedTargets() {
        return switch (this) {
            case DRAFT -> Set.of(PUBLISHED, ARCHIVED);
            case PUBLISHED -> Set.of(RENTED, SOLD, ARCHIVED);
            case RENTED -> Set.of(PUBLISHED, ARCHIVED);
            case SOLD -> Set.of(ARCHIVED);
            case ARCHIVED -> Set.of(PUBLISHED);
        };
    }

    public boolean canTransitionTo(PropertyStatus target) {
        return allowedTargets().contains(target);
    }

    public boolean isEditable() {
        return this == DRAFT || this == PUBLISHED;
    }
}
