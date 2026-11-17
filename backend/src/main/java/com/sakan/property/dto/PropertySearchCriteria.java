package com.sakan.property.dto;

import java.math.BigDecimal;
import java.util.List;

import com.sakan.common.domain.Currency;
import com.sakan.property.domain.PropertyPurpose;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PropertySearchCriteria(
        Long locationId,

        @Min(value = 0, message = "minPrice must be non-negative")
        BigDecimal minPrice,

        @Min(value = 0, message = "maxPrice must be non-negative")
        BigDecimal maxPrice,

        Currency currency,

        Long propertyTypeId,

        PropertyPurpose purpose,

        @Min(value = 0, message = "minBedrooms must be non-negative")
        Integer minBedrooms,

        List<Long> amenityIds,

        @Min(value = 0, message = "Page index must be 0 or greater")
        Integer page,

        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 50, message = "Page size must not exceed 50")
        Integer size,

        String sort
) {
    public PropertySearchCriteria {
        if (page == null) page = 0;
        if (size == null) size = 12;
        if (sort == null || sort.isBlank()) sort = "createdAt,desc";

        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("minPrice cannot be greater than maxPrice");
        }
        if (page < 0) {
            throw new IllegalArgumentException("Page index must be non-negative");
        }
        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("Page size must be between 1 and 50");
        }
    }
}
