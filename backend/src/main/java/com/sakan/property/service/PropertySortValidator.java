package com.sakan.property.service;

import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PropertySortValidator {

    private static final Set<String> WHITELISTED_SORT_FIELDS = Set.of(
            "price",
            "createdAt",
            "bedrooms",
            "areaSqm"
    );

    public Sort validateAndBuildSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"));
        }

        String[] parts = sortParam.split(",");
        String field = parts[0].trim();
        String directionStr = parts.length > 1 ? parts[1].trim().toUpperCase() : "ASC";

        if (!WHITELISTED_SORT_FIELDS.contains(field)) {
            throw new IllegalArgumentException(
                    "Invalid sort field '" + field + "'. Allowed fields are: " + WHITELISTED_SORT_FIELDS
            );
        }

        Sort.Direction direction = "DESC".equals(directionStr) ? Sort.Direction.DESC : Sort.Direction.ASC;

        // Stable tie-breaker with id prevents overlapping results across pages
        return Sort.by(new Sort.Order(direction, field))
                .and(Sort.by(Sort.Order.asc("id")));
    }
}
