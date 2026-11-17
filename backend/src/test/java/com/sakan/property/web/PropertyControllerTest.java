package com.sakan.property.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.sakan.common.domain.Currency;
import com.sakan.common.exception.ForbiddenOperationException;
import com.sakan.common.exception.InvalidStatusTransitionException;
import com.sakan.common.exception.ResourceNotFoundException;
import com.sakan.common.response.PageResponse;
import com.sakan.common.security.AuthenticatedUser;
import com.sakan.common.security.CurrentUserProvider;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;
import com.sakan.property.dto.PropertyRequest;
import com.sakan.property.dto.PropertyResponse;
import com.sakan.property.service.PropertyService;

/**
 * Tests the HTTP layer only: routes, status codes, validation and error mapping. The service and the current-user
 * provider are mocked, so no Spring context, security chain or database is needed.
 */
@ExtendWith(MockitoExtension.class)
class PropertyControllerTest {

    private static final AuthenticatedUser OWNER = new AuthenticatedUser(1L, Set.of(AuthenticatedUser.OWNER));

    private static final String VALID_BODY = """
            {
              "title": "Sea view flat",
              "description": "Bright, 3 minutes from the sea",
              "purpose": "SALE",
              "propertyTypeId": 1,
              "locationId": 5,
              "price": 2500000,
              "currency": "EGP",
              "bedrooms": 3,
              "bathrooms": 2,
              "areaSqm": 140.5,
              "amenityIds": [1, 2]
            }
            """;

    @Mock
    private PropertyService service;

    @Mock
    private CurrentUserProvider currentUser;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new PropertyController(service, currentUser))
                .setValidator(validator)
                .build();
    }

    // ---------- create ----------

    @Test
    void create_validBody_returns201WithLocationHeader() throws Exception {
        when(currentUser.require()).thenReturn(OWNER);
        when(service.create(any(PropertyRequest.class), eq(OWNER))).thenReturn(response(10L));

        mvc.perform(post("/api/properties").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/properties/10"))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currency").value("EGP"));
    }

    @Test
    void create_blankTitle_returns400() throws Exception {
        mvc.perform(post("/api/properties").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"Sea view flat\"", "\"  \"")))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any(), any());
    }

    @Test
    void create_negativePrice_returns400() throws Exception {
        mvc.perform(post("/api/properties").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("2500000", "-5")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingCurrency_returns400() throws Exception {
        mvc.perform(post("/api/properties").contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY.replace("\"currency\": \"EGP\",", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_whenServiceForbids_returns403() throws Exception {
        when(currentUser.require()).thenReturn(OWNER);
        when(service.create(any(PropertyRequest.class), eq(OWNER))).thenThrow(new ForbiddenOperationException("no"));

        mvc.perform(post("/api/properties").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    // ---------- get ----------

    @Test
    void get_asAnonymous_passesNullViewer() throws Exception {
        when(currentUser.find()).thenReturn(Optional.empty());
        when(service.getById(eq(10L), isNull())).thenReturn(response(10L));

        mvc.perform(get("/api/properties/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void get_asLoggedInUser_passesTheViewer() throws Exception {
        when(currentUser.find()).thenReturn(Optional.of(OWNER));
        when(service.getById(10L, OWNER)).thenReturn(response(10L));

        mvc.perform(get("/api/properties/10")).andExpect(status().isOk());
    }

    @Test
    void get_unknownOrHiddenListing_returns404() throws Exception {
        when(currentUser.find()).thenReturn(Optional.empty());
        when(service.getById(eq(99L), isNull())).thenThrow(new ResourceNotFoundException("Property 99 not found"));

        mvc.perform(get("/api/properties/99")).andExpect(status().isNotFound());
    }

    // ---------- mine ----------

    @Test
    void mine_passesPagingParametersAndReturnsPage() throws Exception {
        when(currentUser.require()).thenReturn(OWNER);
        when(service.listMine(OWNER, 2, 10))
                .thenReturn(new PageResponse<>(List.of(response(10L)), 2, 10, 21, 3));

        mvc.perform(get("/api/properties/mine").param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.totalElements").value(21))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void mine_isNotConfusedWithGetById() throws Exception {
        when(currentUser.require()).thenReturn(OWNER);
        when(service.listMine(OWNER, 0, 20)).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mvc.perform(get("/api/properties/mine")).andExpect(status().isOk());
    }

    // ---------- update ----------

    @Test
    void update_validBody_returns200() throws Exception {
        when(currentUser.require()).thenReturn(OWNER);
        when(service.update(eq(10L), any(PropertyRequest.class), eq(OWNER))).thenReturn(response(10L));

        mvc.perform(put("/api/properties/10").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk());
    }

    @Test
    void update_byNonOwner_returns403() throws Exception {
        when(currentUser.require()).thenReturn(OWNER);
        when(service.update(eq(10L), any(PropertyRequest.class), eq(OWNER)))
                .thenThrow(new ForbiddenOperationException("not yours"));

        mvc.perform(put("/api/properties/10").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());
    }

    // ---------- publish / archive ----------

    @Test
    void publish_returns200() throws Exception {
        when(currentUser.require()).thenReturn(OWNER);
        when(service.publish(10L, OWNER)).thenReturn(response(10L, PropertyStatus.PUBLISHED));

        mvc.perform(post("/api/properties/10/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void publish_illegalTransition_returns409() throws Exception {
        when(currentUser.require()).thenReturn(OWNER);
        when(service.publish(10L, OWNER)).thenThrow(new InvalidStatusTransitionException("ARCHIVED -> PUBLISHED"));

        mvc.perform(post("/api/properties/10/publish")).andExpect(status().isConflict());
    }

    @Test
    void archive_returns200() throws Exception {
        when(currentUser.require()).thenReturn(OWNER);
        when(service.archive(10L, OWNER)).thenReturn(response(10L, PropertyStatus.ARCHIVED));

        mvc.perform(post("/api/properties/10/archive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ARCHIVED"));
    }

    @Test
    void archive_illegalTransition_returns409() throws Exception {
        when(currentUser.require()).thenReturn(OWNER);
        when(service.archive(10L, OWNER)).thenThrow(new InvalidStatusTransitionException("ARCHIVED -> ARCHIVED"));

        mvc.perform(post("/api/properties/10/archive")).andExpect(status().isConflict());
    }

    // ---------- helpers ----------

    private static PropertyResponse response(Long id) {
        return response(id, PropertyStatus.DRAFT);
    }

    private static PropertyResponse response(Long id, PropertyStatus status) {
        return new PropertyResponse(
                id, 1L, null, "Sea view flat", "Bright", PropertyPurpose.SALE, 1L, 5L,
                new BigDecimal("2500000.00"), Currency.EGP, 3, 2, new BigDecimal("140.50"),
                Set.of(1L, 2L), status, Instant.parse("2026-11-17T12:00:00Z"), Instant.parse("2026-11-17T12:00:00Z"));
    }
}
