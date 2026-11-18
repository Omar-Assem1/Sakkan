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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.sakan.common.domain.Currency;
import com.sakan.common.exception.GlobalExceptionHandler;
import com.sakan.common.response.PageResponse;
import com.sakan.common.security.CurrentUserProvider;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;
import com.sakan.property.dto.PropertySummaryResponse;
import com.sakan.property.service.PropertyService;
import com.sakan.support.tags.RegressionTest;

@RegressionTest
@ExtendWith(MockitoExtension.class)
class PropertySecurityTest {

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
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "' OR 1=1 --",
            "price; DROP TABLE property;",
            "1; SELECT pg_sleep(5);",
            "bedrooms,asc; DELETE FROM property;"
    })
    @DisplayName("SQL Injection in sort parameter returns 400 Bad Request, never 500")
    void testSqlInjectionInSortReturns400(String maliciousPayload) throws Exception {
        mvc.perform(get("/api/properties")
                        .param("sort", maliciousPayload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    @DisplayName("Boundary test: page size 50 is accepted (200 OK)")
    void testPageSizeMaxBoundary50() throws Exception {
        when(propertyService.browse(any())).thenReturn(
                new PageResponse<>(List.of(), 0, 50, 0, 0)
        );

        mvc.perform(get("/api/properties")
                        .param("size", "50")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(50));
    }

    @Test
    @DisplayName("Boundary test: page size 51 is rejected (400 Bad Request)")
    void testPageSizeOverMaxRejected() throws Exception {
        mvc.perform(get("/api/properties")
                        .param("size", "51")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Page size must be between 1 and 50"));
    }

    @Test
    @DisplayName("Anti-leakage: Response DTO strictly omits ownerId, agentId, and internal fields")
    void testResponseOmitsSensitiveInternalFields() throws Exception {
        PropertySummaryResponse summary = new PropertySummaryResponse(
                42L, "Penthouse", "Luxury living",
                PropertyPurpose.SALE, 1L, 10L,
                new BigDecimal("5000000.00"), Currency.EGP,
                4, 3, new BigDecimal("250.00"),
                Set.of(1L, 2L), PropertyStatus.PUBLISHED,
                Instant.now()
        );

        when(propertyService.browse(any())).thenReturn(
                new PageResponse<>(List.of(summary), 0, 12, 1, 1)
        );

        mvc.perform(get("/api/properties")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(42))
                .andExpect(jsonPath("$.content[0].ownerId").doesNotExist())
                .andExpect(jsonPath("$.content[0].agentId").doesNotExist());
    }
}
