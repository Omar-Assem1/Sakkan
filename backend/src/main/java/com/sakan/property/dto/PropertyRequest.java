package com.sakan.property.dto;

import java.math.BigDecimal;
import java.util.Set;

import com.sakan.common.domain.Currency;
import com.sakan.property.domain.PropertyPurpose;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PropertyRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 5000) String description,
        @NotNull PropertyPurpose purpose,
        @NotNull @Positive Long propertyTypeId,
        @NotNull @Positive Long locationId,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal price,
        @NotNull Currency currency,
        @NotNull @Min(0) @Max(50) Integer bedrooms,
        @NotNull @Min(0) @Max(50) Integer bathrooms,
        @NotNull @Positive @Digits(integer = 8, fraction = 2) BigDecimal areaSqm,
        Set<Long> amenityIds) {

    public PropertyRequest {
        amenityIds = amenityIds == null ? Set.of() : Set.copyOf(amenityIds);
    }
}
