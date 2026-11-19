package com.sakan.viewing.dto;

import java.time.Instant;

import com.sakan.viewing.domain.ViewingStatus;

/** API response representing a viewing appointment (FR-17, FR-21). */
public record ViewingResponse(
        Long id,
        Long slotId,
        Long propertyId,
        Long buyerId,
        ViewingStatus status,
        String notes,
        Instant createdAt,
        Instant updatedAt) {
}
