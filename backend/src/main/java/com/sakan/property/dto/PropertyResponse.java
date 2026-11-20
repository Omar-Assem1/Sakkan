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
        Instant updatedAt,
        String thumbnailUrl) {

    public PropertyResponse(
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
        this(id, ownerId, agentId, title, description, purpose, propertyTypeId, locationId,
                price, currency, bedrooms, bathrooms, areaSqm, amenityIds, status, createdAt, updatedAt, null);
    }

    public static PropertyResponse withThumbnailUrl(PropertyResponse base, String thumbnailUrl) {
        return new PropertyResponse(base.id(), base.ownerId(), base.agentId(), base.title(), base.description(),
                base.purpose(), base.propertyTypeId(), base.locationId(), base.price(), base.currency(),
                base.bedrooms(), base.bathrooms(), base.areaSqm(), base.amenityIds(), base.status(),
                base.createdAt(), base.updatedAt(), thumbnailUrl);
    }
}
