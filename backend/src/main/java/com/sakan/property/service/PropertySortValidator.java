package com.sakan.property.service;

import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Validates sort parameters against a strict whitelist to prevent SQL injection
 * and invalid column queries. Maps client aliases (e.g. areaM2 -> areaSqm) and enforces
 * a deterministic 'id ASC' tie-breaker to prevent pagination overlap (NFR-01, NFR-04).
 */
@Component
public class PropertySortValidator {

    private static final Map<String, String> FIELD_MAPPING = Map.of(
            "price", "price",
            "createdAt", "createdAt",
            "bedrooms", "bedrooms",
            "areaSqm", "areaSqm",
            "areaM2", "areaSqm" // Client alias mapped to entity property
    );

    private static final Set<String> WHITELISTED_SORT_FIELDS = FIELD_MAPPING.keySet();

    public Sort validateAndBuildSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"));
        }

        String[] parts = sortParam.split(",");
        if (parts.length > 2) {
            throw new IllegalArgumentException("Invalid sort format: expected 'field' or 'field,direction'");
        }

        String rawField = parts[0].trim();
        String directionStr = parts.length > 1 ? parts[1].trim().toUpperCase() : "ASC";

        if (!WHITELISTED_SORT_FIELDS.contains(rawField)) {
            throw new IllegalArgumentException(
                    "Invalid sort field '" + rawField + "'. Allowed fields are: " + WHITELISTED_SORT_FIELDS
            );
        }

        if (parts.length > 1 && !("ASC".equals(directionStr) || "DESC".equals(directionStr))) {
            throw new IllegalArgumentException(
                    "Invalid sort direction '" + parts[1].trim() + "'. Allowed values are: ASC, DESC"
            );
        }

        String entityField = FIELD_MAPPING.get(rawField);
        Sort.Direction direction = "DESC".equals(directionStr) ? Sort.Direction.DESC : Sort.Direction.ASC;

        // Stable tie-breaker with 'id ASC' ensures deterministic pagination across pages
        return Sort.by(new Sort.Order(direction, entityField))
                .and(Sort.by(Sort.Order.asc("id")));
    }
}
