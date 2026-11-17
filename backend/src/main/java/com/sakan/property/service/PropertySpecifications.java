package com.sakan.property.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.sakan.common.domain.Currency;
import com.sakan.property.domain.Property;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

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
}
