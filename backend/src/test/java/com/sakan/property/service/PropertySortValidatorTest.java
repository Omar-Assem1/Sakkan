package com.sakan.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.Sort;

class PropertySortValidatorTest {

    private PropertySortValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PropertySortValidator();
    }

    @Test
    @DisplayName("Default sort when null or blank should be createdAt DESC, id ASC")
    void testDefaultSort() {
        Sort sortNull = validator.validateAndBuildSort(null);
        assertThat(sortNull.getOrderFor("createdAt")).isNotNull();
        assertThat(sortNull.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(sortNull.getOrderFor("id")).isNotNull();
        assertThat(sortNull.getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);

        Sort sortBlank = validator.validateAndBuildSort("   ");
        assertThat(sortBlank.getOrderFor("createdAt")).isNotNull();
        assertThat(sortBlank.getOrderFor("id")).isNotNull();
    }

    @Test
    @DisplayName("Valid whitelist fields with explicit ASC/DESC")
    void testValidSortFields() {
        Sort priceDesc = validator.validateAndBuildSort("price,desc");
        assertThat(priceDesc.getOrderFor("price").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(priceDesc.getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);

        Sort bedroomsAsc = validator.validateAndBuildSort("bedrooms,asc");
        assertThat(bedroomsAsc.getOrderFor("bedrooms").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(bedroomsAsc.getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("Alias 'areaM2' should map cleanly to entity field 'areaSqm'")
    void testAreaM2AliasMapping() {
        Sort sort = validator.validateAndBuildSort("areaM2,desc");
        assertThat(sort.getOrderFor("areaSqm")).isNotNull();
        assertThat(sort.getOrderFor("areaSqm").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(sort.getOrderFor("id").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ownerId,desc",
            "agentId,asc",
            "password,desc",
            "status,asc",
            "price; DROP TABLE property;",
            "' OR 1=1 --",
            "UNION SELECT * FROM users",
            "unknownField",
            "1=1"
    })
    @DisplayName("Rejects non-whitelisted fields and SQL injection attempts with IllegalArgumentException")
    void testRejectInvalidAndMaliciousSortFields(String maliciousSort) {
        assertThatThrownBy(() -> validator.validateAndBuildSort(maliciousSort))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid sort field");
    }
}
