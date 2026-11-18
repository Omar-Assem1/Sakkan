package com.sakan.property.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;

import com.sakan.TestcontainersConfiguration;
import com.sakan.common.domain.Currency;
import com.sakan.property.domain.Property;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;
import com.sakan.property.service.PropertySpecifications;
import com.sakan.support.tags.IntegrationTest;
import com.sakan.support.tags.RegressionTest;

import jakarta.persistence.EntityManager;

@RegressionTest
@IntegrationTest
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class PropertyBrowseRepositoryTest {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private EntityManager entityManager;

    private Long prop1Id;
    private Long prop2Id;

    @BeforeEach
    void setUp() {
        Property p1 = new Property();
        p1.setTitle("P1 Published Smouha");
        p1.setDescription("Close to green spaces");
        p1.setOwnerId(100L);
        p1.setLocationId(10L);
        p1.setPropertyTypeId(1L);
        p1.setPrice(new BigDecimal("2000000.00"));
        p1.setCurrency(Currency.EGP);
        p1.setPurpose(PropertyPurpose.SALE);
        p1.setBedrooms(3);
        p1.setBathrooms(2);
        p1.setAreaSqm(new BigDecimal("140.00"));
        p1.setStatus(PropertyStatus.PUBLISHED);
        p1.setAmenityIds(Set.of(1L, 2L));
        entityManager.persist(p1);
        prop1Id = p1.getId();

        Property p2 = new Property();
        p2.setTitle("P2 Published Kafr Abdo");
        p2.setDescription("Spacious duplex");
        p2.setOwnerId(101L);
        p2.setLocationId(11L);
        p2.setPropertyTypeId(2L);
        p2.setPrice(new BigDecimal("3500000.00"));
        p2.setCurrency(Currency.EGP);
        p2.setPurpose(PropertyPurpose.SALE);
        p2.setBedrooms(4);
        p2.setBathrooms(3);
        p2.setAreaSqm(new BigDecimal("260.00"));
        p2.setStatus(PropertyStatus.PUBLISHED);
        p2.setAmenityIds(Set.of(1L, 2L, 3L));
        entityManager.persist(p2);
        prop2Id = p2.getId();

        Property p3 = new Property();
        p3.setTitle("P3 Draft Property");
        p3.setDescription("Work in progress");
        p3.setOwnerId(100L);
        p3.setLocationId(10L);
        p3.setPropertyTypeId(1L);
        p3.setPrice(new BigDecimal("1500000.00"));
        p3.setCurrency(Currency.EGP);
        p3.setPurpose(PropertyPurpose.SALE);
        p3.setBedrooms(2);
        p3.setBathrooms(1);
        p3.setAreaSqm(new BigDecimal("90.00"));
        p3.setStatus(PropertyStatus.DRAFT);
        p3.setAmenityIds(Set.of(1L));
        entityManager.persist(p3);

        Property p4 = new Property();
        p4.setTitle("P4 Rent Flat");
        p4.setDescription("Available for immediate rent");
        p4.setOwnerId(102L);
        p4.setLocationId(5L);
        p4.setPropertyTypeId(1L);
        p4.setPrice(new BigDecimal("8000.00"));
        p4.setCurrency(Currency.EGP);
        p4.setPurpose(PropertyPurpose.RENT);
        p4.setBedrooms(2);
        p4.setBathrooms(1);
        p4.setAreaSqm(new BigDecimal("100.00"));
        p4.setStatus(PropertyStatus.PUBLISHED);
        p4.setAmenityIds(Set.of(2L));
        entityManager.persist(p4);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Filter: Only PUBLISHED properties are returned, DRAFT is strictly excluded")
    void testOnlyPublishedReturned() {
        Specification<Property> spec = PropertySpecifications.isPublished();
        List<Property> results = propertyRepository.findAll(spec);

        assertThat(results).hasSize(3);
        assertThat(results).noneMatch(p -> p.getStatus() == PropertyStatus.DRAFT);
    }

    @Test
    @DisplayName("Filter: Price bounds inclusive and currency matching")
    void testPriceBoundsInclusive() {
        Specification<Property> spec = PropertySpecifications.isPublished()
                .and(PropertySpecifications.hasPriceBetween(new BigDecimal("2000000.00"), new BigDecimal("3500000.00"), Currency.EGP));

        List<Property> results = propertyRepository.findAll(spec);
        assertThat(results).hasSize(2);
        assertThat(results).extracting(Property::getId).containsExactlyInAnyOrder(prop1Id, prop2Id);
    }

    @Test
    @DisplayName("Filter: Location descendants filter matches all provided IDs in hierarchy")
    void testLocationDescendants() {
        Specification<Property> spec = PropertySpecifications.isPublished()
                .and(PropertySpecifications.inLocationIds(List.of(10L, 11L)));

        List<Property> results = propertyRepository.findAll(spec);
        assertThat(results).hasSize(2);
    }

    @Test
    @DisplayName("Filter: Amenities hasAll matching ensures all selected amenity IDs exist")
    void testAmenitiesHasAll() {
        Specification<Property> spec12 = PropertySpecifications.isPublished()
                .and(PropertySpecifications.hasAllAmenities(List.of(1L, 2L)));
        List<Property> results12 = propertyRepository.findAll(spec12);
        assertThat(results12).hasSize(2);

        Specification<Property> spec123 = PropertySpecifications.isPublished()
                .and(PropertySpecifications.hasAllAmenities(List.of(1L, 2L, 3L)));
        List<Property> results123 = propertyRepository.findAll(spec123);
        assertThat(results123).hasSize(1);
        assertThat(results123.getFirst().getId()).isEqualTo(prop2Id);
    }

    @Test
    @DisplayName("Pagination and Sort: Deterministic ordering with id tie-breaker prevents overlapping")
    void testPaginationAndSort() {
        Specification<Property> spec = PropertySpecifications.isPublished()
                .and(PropertySpecifications.hasPurpose(PropertyPurpose.SALE));

        Sort sort = Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id"));
        PageRequest pageRequest = PageRequest.of(0, 1, sort);

        Page<Property> page = propertyRepository.findAll(spec, pageRequest);
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getId()).isEqualTo(prop2Id);
    }

    @Test
    @DisplayName("Pagination: Beyond range returns empty content array with totalElements preserved")
    void testBeyondRangeReturnsEmpty() {
        Specification<Property> spec = PropertySpecifications.isPublished();
        PageRequest pageRequest = PageRequest.of(10, 10, Sort.by("id"));

        Page<Property> page = propertyRepository.findAll(spec, pageRequest);
        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(3);
    }
}
