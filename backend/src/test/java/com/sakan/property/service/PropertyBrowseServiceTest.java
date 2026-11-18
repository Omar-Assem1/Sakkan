package com.sakan.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.sakan.common.domain.Currency;
import com.sakan.common.response.PageResponse;
import com.sakan.property.domain.Property;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;
import com.sakan.property.dto.PropertyMapper;
import com.sakan.property.dto.PropertySearchCriteria;
import com.sakan.property.dto.PropertySummaryResponse;
import com.sakan.property.repository.PropertyRepository;
import com.sakan.support.tags.RegressionTest;

@RegressionTest
@ExtendWith(MockitoExtension.class)
class PropertyBrowseServiceTest {

    @Mock
    private PropertyRepository repository;

    @Mock
    private PropertyMapper mapper;

    @Mock
    private LocationHierarchyService locationHierarchyService;

    @Mock
    private PropertySortValidator sortValidator;

    @InjectMocks
    private PropertyService propertyService;

    @Test
    @DisplayName("browse() coordinates location hierarchy expansion, sorting, specification query, and DTO projection")
    void testBrowseCoordinatesSuccessfully() {
        PropertySearchCriteria criteria = new PropertySearchCriteria(
                5L,
                new BigDecimal("1000000.00"),
                new BigDecimal("3000000.00"),
                Currency.EGP,
                1L,
                PropertyPurpose.SALE,
                2,
                List.of(1L, 2L),
                0,
                12,
                "price,desc"
        );

        when(sortValidator.validateAndBuildSort("price,desc"))
                .thenReturn(Sort.by(Sort.Order.desc("price"), Sort.Order.asc("id")));
        when(locationHierarchyService.findDescendantLocationIds(5L))
                .thenReturn(List.of(5L, 10L, 11L));

        Property property = new Property();
        property.setTitle("P1");
        property.setStatus(PropertyStatus.PUBLISHED);

        Page<Property> pagedProperties = new PageImpl<>(List.of(property));
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pagedProperties);

        PropertySummaryResponse summary = new PropertySummaryResponse(
                1L, "P1", "Desc", PropertyPurpose.SALE, 1L, 10L,
                new BigDecimal("2000000.00"), Currency.EGP, 3, 2,
                new BigDecimal("140.00"), Set.of(1L, 2L), PropertyStatus.PUBLISHED, Instant.now()
        );
        when(mapper.toSummaryResponse(property)).thenReturn(summary);

        PageResponse<PropertySummaryResponse> response = propertyService.browse(criteria);

        verify(sortValidator).validateAndBuildSort("price,desc");
        verify(locationHierarchyService).findDescendantLocationIds(5L);
        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content().getFirst().title()).isEqualTo("P1");
    }
}
