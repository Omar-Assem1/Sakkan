package com.sakan.viewing.dto;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

/** Body of POST /api/properties/{propertyId}/slots. endTime > startTime is checked in the service. */
public record CreateViewingSlotRequest(
        @Schema(description = "Slot start (ISO-8601, UTC)", example = "2026-12-01T10:00:00Z")
        @NotNull @Future Instant startTime,
        @Schema(description = "Slot end (ISO-8601, UTC), must be after startTime", example = "2026-12-01T11:00:00Z")
        @NotNull @Future Instant endTime) {
}
