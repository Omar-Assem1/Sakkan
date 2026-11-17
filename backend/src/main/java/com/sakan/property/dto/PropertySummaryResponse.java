package com.sakan.property.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import com.sakan.common.domain.Currency;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;

/**
 * Public DTO for property browsing. Excludes ownerId and agentId to avoid leaking user internals (NFR-04).
 */
public record PropertySummaryResponse(
        Long id,
        String title,
        String description,
        PropertyPurpose purpose,
        Long propertyTypeId,
        Long locationId,
        BigDecimal price,
        Currency currency,
        int bedrooms,
        int bathrooms,
        BigDecimal areaSqm,
        Set<Long> amenityIds,
        PropertyStatus status,
        Instant createdAt) {
}
