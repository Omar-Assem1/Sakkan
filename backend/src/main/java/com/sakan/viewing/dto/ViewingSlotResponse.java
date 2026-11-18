package com.sakan.viewing.dto;

import java.time.Instant;

public record ViewingSlotResponse(
        Long id,
        Long propertyId,
        Long createdBy,
        Instant startTime,
        Instant endTime,
        Instant createdAt) {
}
