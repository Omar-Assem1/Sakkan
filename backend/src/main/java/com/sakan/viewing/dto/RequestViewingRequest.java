package com.sakan.viewing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body of POST /api/viewings (Form 6, FR-17). */
public record RequestViewingRequest(
        @Schema(description = "ID of the available viewing slot to book", example = "42")
        @NotNull Long slotId,
        @Schema(description = "Optional note for the property owner or agent", example = "Prefer afternoon call before arrival")
        @Size(max = 1000) String notes) {
}
