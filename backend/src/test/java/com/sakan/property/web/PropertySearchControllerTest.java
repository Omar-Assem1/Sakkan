package com.sakan.property.web;

import static org.hamcrest.Matchers.is;
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
import com.sakan.common.exception.GlobalExceptionHandler;
import com.sakan.common.response.PageResponse;
import com.sakan.common.security.CurrentUserProvider;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;
import com.sakan.property.dto.PropertySummaryResponse;
import com.sakan.property.service.PropertyService;
import com.sakan.support.tags.RegressionTest;
import com.sakan.support.tags.UnitTest;

@UnitTest
@RegressionTest
@ExtendWith(MockitoExtension.class)
class PropertySearchControllerTest {

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

    @Test
    @DisplayName("SCRUM-88: Valid 'q' parameter returns 200 with PageResponse")
    void testValidSearchQueryReturns200() throws Exception {
        PropertySummaryResponse summary = new PropertySummaryResponse(
                1L, "Smouha Flat", "Near sporting",
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
                        .param("q", "Smouha")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Smouha Flat"));
    }

    @Test
    @DisplayName("SCRUM-88: Over-long search query (> 100 characters) returns 400 Bad Request ProblemDetail")
    void testOverLongSearchQueryReturns400() throws Exception {
        String overLong = "a".repeat(101);

        mvc.perform(get("/api/properties")
                        .param("q", overLong)
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title", is("Bad Request")));
    }
}
