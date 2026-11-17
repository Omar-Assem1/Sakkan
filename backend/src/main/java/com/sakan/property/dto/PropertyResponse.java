package com.sakan.property.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;

import com.sakan.common.domain.Currency;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;


public record PropertyResponse(
        Long id,
        Long ownerId,
        Long agentId,
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
        Instant createdAt,
        Instant updatedAt) {
}
