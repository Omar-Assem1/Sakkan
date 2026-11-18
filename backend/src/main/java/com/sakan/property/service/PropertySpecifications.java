package com.sakan.property.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.sakan.common.domain.Currency;
import com.sakan.property.domain.Property;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public final class PropertySpecifications {

    private PropertySpecifications() {}

    public static Specification<Property> isPublished() {
        return (root, query, cb) -> cb.equal(root.get("status"), PropertyStatus.PUBLISHED);
    }

    public static Specification<Property> inLocationIds(List<Long> locationIds) {
        return (root, query, cb) -> {
            if (locationIds == null || locationIds.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("locationId").in(locationIds);
        };
    }

    public static Specification<Property> hasPriceBetween(BigDecimal minPrice, BigDecimal maxPrice, Currency currency) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (currency != null) {
                predicates = cb.and(predicates, cb.equal(root.get("currency"), currency));
            }
            if (minPrice != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            return predicates;
        };
    }

    public static Specification<Property> hasPropertyTypeId(Long propertyTypeId) {
        return (root, query, cb) -> propertyTypeId == null ? cb.conjunction() : cb.equal(root.get("propertyTypeId"), propertyTypeId);
    }

    public static Specification<Property> hasPurpose(PropertyPurpose purpose) {
        return (root, query, cb) -> purpose == null ? cb.conjunction() : cb.equal(root.get("purpose"), purpose);
    }

    public static Specification<Property> hasMinBedrooms(Integer minBedrooms) {
        return (root, query, cb) -> (minBedrooms == null || minBedrooms <= 0)
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("bedrooms"), minBedrooms);
    }

    public static Specification<Property> hasAllAmenities(List<Long> amenityIds) {
        return (root, query, cb) -> {
            if (amenityIds == null || amenityIds.isEmpty()) {
                return cb.conjunction();
            }

            query.distinct(true);
            Join<Property, Long> amenitiesJoin = root.join("amenityIds", JoinType.INNER);

            query.groupBy(root.get("id"));
            query.having(cb.equal(cb.countDistinct(amenitiesJoin), (long) amenityIds.size()));

            return amenitiesJoin.in(amenityIds);
        };
    }

    /**
     * Full-text search and fuzzy typo-tolerant matching across title and description (FR-17, SCRUM-87).
     * Strictly parameterised via CriteriaBuilder (NFR-04).
     */
    public static Specification<Property> hasSearch(String rawQuery) {
        return (root, query, cb) -> {
            if (rawQuery == null || rawQuery.isBlank()) {
                return cb.conjunction();
            }

            String clean = rawQuery.trim();
            String lowerPattern = "%" + clean.toLowerCase() + "%";

            // 1. Case-insensitive substring match on title and description
            Predicate titleLike = cb.like(cb.lower(root.get("title")), lowerPattern);
            Predicate descLike = cb.like(cb.lower(cb.coalesce(root.get("description"), "")), lowerPattern);

            // 2. Trigram similarity match on title for typo tolerance (threshold >= 0.3)
            Expression<Double> titleSimilarity = cb.function(
                    "similarity",
                    Double.class,
                    root.get("title"),
                    cb.literal(clean)
            );
            Predicate trigramTitle = cb.greaterThanOrEqualTo(titleSimilarity, 0.3);

            // 3. Arabic normalisation match: strips tashkeel, unifies Alef variants and Taa Marbouta
            String normQuery = normalizeArabic(clean);
            String lowerNormPattern = "%" + normQuery.toLowerCase() + "%";

            Expression<String> strippedTitle = cb.function(
                    "regexp_replace",
                    String.class,
                    root.get("title"),
                    cb.literal("[\\u064B-\\u0652\\u0670]"),
                    cb.literal(""),
                    cb.literal("g")
            );
            Expression<String> normTitle = cb.function(
                    "translate",
                    String.class,
                    strippedTitle,
                    cb.literal("أإآة"),
                    cb.literal("اااه")
            );
            Predicate arabicTitleLike = cb.like(cb.lower(normTitle), lowerNormPattern);

            Expression<String> strippedDesc = cb.function(
                    "regexp_replace",
                    String.class,
                    cb.coalesce(root.get("description"), ""),
                    cb.literal("[\\u064B-\\u0652\\u0670]"),
                    cb.literal(""),
                    cb.literal("g")
            );
            Expression<String> normDesc = cb.function(
                    "translate",
                    String.class,
                    strippedDesc,
                    cb.literal("أإآة"),
                    cb.literal("اااه")
            );
            Predicate arabicDescLike = cb.like(cb.lower(normDesc), lowerNormPattern);

            // Relevance ordering when relevance sort is selected (avoid on count query)
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.orderBy(cb.desc(titleSimilarity), cb.desc(root.get("createdAt")), cb.asc(root.get("id")));
            }

            return cb.or(titleLike, descLike, trigramTitle, arabicTitleLike, arabicDescLike);
        };
    }

    public static String normalizeArabic(String text) {
        if (text == null) return "";
        return text
                .replaceAll("[\\u064B-\\u0652\\u0670]", "")
                .replaceAll("[\\u0622\\u0623\\u0625]", "\u0627")
                .replace('\u0629', '\u0647');
    }
}
