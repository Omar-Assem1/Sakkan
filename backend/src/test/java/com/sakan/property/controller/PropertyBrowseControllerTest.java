package com.sakan.property.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.sakan.common.domain.Currency;
import com.sakan.common.response.PageResponse;
import com.sakan.common.security.CurrentUserProvider;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;
import com.sakan.property.dto.PropertySummaryResponse;
import com.sakan.property.service.PropertyService;

@ExtendWith(MockitoExtension.class)
class PropertyBrowseControllerTest {

    @Mock
    private PropertyService propertyService;

    @Mock
    private CurrentUserProvider currentUser;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new PropertyController(propertyService, currentUser))
                .setValidator(validator)
                .setControllerAdvice(new com.sakan.common.exception.GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("200 OK: Valid query params return 200 with PageResponse shape")
    void testBrowseProperties200() throws Exception {
        PropertySummaryResponse summary = new PropertySummaryResponse(
                1L, "Modern Flat", "Description",
                PropertyPurpose.SALE, 1L, 10L,
                new BigDecimal("2500000.00"), Currency.EGP,
                3, 2, new BigDecimal("145.00"),
                Set.of(1L, 2L), PropertyStatus.PUBLISHED,
                Instant.now()
        );

        when(propertyService.browse(any())).thenReturn(
                new PageResponse<>(List.of(summary), 0, 12, 1L, 1)
        );

        mvc.perform(get("/api/properties")
                        .param("minPrice", "1000000")
                        .param("maxPrice", "3000000")
                        .param("currency", "EGP")
                        .param("page", "0")
                        .param("size", "12")
                        .param("sort", "price,desc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].price").value(2500000.00));
    }

    @Test
    @DisplayName("400 Bad Request: minPrice > maxPrice")
    void testMinPriceGreaterThanMaxPriceReturns400() throws Exception {
        mvc.perform(get("/api/properties")
                        .param("minPrice", "5000000")
                        .param("maxPrice", "2000000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 Bad Request: size > 50")
    void testSizeGreaterThan50Returns400() throws Exception {
        mvc.perform(get("/api/properties")
                        .param("size", "51"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 Bad Request: size <= 0")
    void testZeroSizeReturns400() throws Exception {
        mvc.perform(get("/api/properties")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 Bad Request: negative page")
    void testNegativePageReturns400() throws Exception {
        mvc.perform(get("/api/properties")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 Bad Request: Invalid sort field rejected by validator")
    void testInvalidSortFieldReturns400() throws Exception {
        mvc.perform(get("/api/properties")
                        .param("sort", "ownerId,desc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("400 Bad Request: Invalid enum parameter")
    void testInvalidEnumReturns400() throws Exception {
        mvc.perform(get("/api/properties")
                        .param("purpose", "INVALID_PURPOSE"))
                .andExpect(status().isBadRequest());
    }
}
