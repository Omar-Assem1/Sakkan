package com.sakan.property.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import com.sakan.property.dto.PropertyMapper;
import com.sakan.property.repository.PropertyRepository;
import com.sakan.support.tags.RegressionTest;
import com.sakan.support.tags.UnitTest;

@UnitTest
@RegressionTest
@ExtendWith(MockitoExtension.class)
class PropertySearchServiceTest {

    @Mock
    private PropertyRepository repository;

    @Mock
    private PropertyMapper mapper;

    @Mock
    private LocationHierarchyService locationHierarchyService;

    private PropertySortValidator sortValidator;

    @InjectMocks
    private PropertyService propertyService;

    @BeforeEach
    void setUp() {
        sortValidator = new PropertySortValidator();
    }

    @Test
    @DisplayName("SCRUM-88: Sort validator supports 'relevance' sort for full-text search results")
    void testSortValidatorSupportsRelevanceSort() {
        Sort sort = sortValidator.validateAndBuildSort("relevance,desc");
        assertThat(sort.getOrderFor("relevance")).isNotNull();
        assertThat(sort.getOrderFor("relevance").getDirection()).isEqualTo(Sort.Direction.DESC);
    }
}
