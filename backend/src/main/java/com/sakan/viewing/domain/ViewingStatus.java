package com.sakan.viewing.domain;

import java.util.Set;

/** Lifecycle states of a viewing appointment (FR-17, FR-19, FR-20). */
public enum ViewingStatus {
    REQUESTED,
    CONFIRMED,
    CANCELLED,
    COMPLETED;

    public Set<ViewingStatus> allowedTargets() {
        return switch (this) {
            case REQUESTED -> Set.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> Set.of(CANCELLED, COMPLETED);
            case CANCELLED, COMPLETED -> Set.of();
        };
    }
}
