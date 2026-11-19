package com.sakan.common.event;

import java.time.Instant;

/** Published when a feature completes a valid status transition. */
public record StatusChangedEvent(
        String entityType,
        String entityId,
        String previousStatus,
        String newStatus,
        String actor,
        Instant occurredAt) {
}
