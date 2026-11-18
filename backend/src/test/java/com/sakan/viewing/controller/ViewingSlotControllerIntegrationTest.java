package com.sakan.viewing.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.sakan.common.domain.Currency;
import com.sakan.property.domain.Property;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;
import com.sakan.property.repository.PropertyRepository;
import com.sakan.support.AbstractIntegrationTest;
import com.sakan.support.security.EndpointSecurityAssertions;
import com.sakan.support.security.Role;
import com.sakan.support.tags.IntegrationTest;

@IntegrationTest
@Transactional
class ViewingSlotControllerIntegrationTest extends AbstractIntegrationTest {

    private static final long OWNER_ID = 101L;
    private static final long ASSIGNED_AGENT_ID = 102L;
    private static final long OTHER_OWNER_ID = 999L;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PropertyRepository propertyRepository;

    private MockMvc mvc;
    private Property publishedProperty;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Property p = new Property();
        p.setOwnerId(OWNER_ID);
        p.setAgentId(ASSIGNED_AGENT_ID);
        p.setTitle("Smouha Viewing Test Flat");
        p.setDescription("Published property for viewing slots IT");
        p.setPurpose(PropertyPurpose.SALE);
        p.setPropertyTypeId(1L);
        p.setLocationId(1L);
        p.setPrice(new BigDecimal("3000000.00"));
        p.setCurrency(Currency.EGP);
        p.setBedrooms(3);
        p.setBathrooms(2);
        p.setAreaSqm(new BigDecimal("150.00"));
        p.setStatus(PropertyStatus.PUBLISHED);
        publishedProperty = propertyRepository.saveAndFlush(p);
    }

    @Test
    @DisplayName("Unauthenticated POST/DELETE returns 401 and BUYER_TENANT receives 403 Forbidden")
    void security_unauthenticatedAndBuyerTenantRejected() throws Exception {
        String url = "/api/properties/" + publishedProperty.getId() + "/slots";
        EndpointSecurityAssertions.assertUnauthorized(mvc, HttpMethod.POST, url);
        EndpointSecurityAssertions.assertForbiddenFor(
                mvc, HttpMethod.POST, url, Role.BUYER_TENANT, Role.MAINTENANCE_STAFF);
    }

    @Test
    @DisplayName("Adjacent slots (10:00-11:00 and 11:00-12:00) succeed with 201 Created, while overlapping slots fail with 409 ProblemDetail")
    void createSlots_adjacentSucceed_andOverlappingRejectedWith409() throws Exception {
        Instant tenAm = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant elevenAm = tenAm.plus(1, ChronoUnit.HOURS);
        Instant twelvePm = tenAm.plus(2, ChronoUnit.HOURS);
        Instant overlapStart = tenAm.plus(30, ChronoUnit.MINUTES);
        Instant overlapEnd = elevenAm.plus(30, ChronoUnit.MINUTES);

        String url = "/api/properties/" + publishedProperty.getId() + "/slots";

        // 1. First slot 10:00-11:00 by OWNER -> 201 Created
        mvc.perform(post(url)
                        .with(csrf())
                        .with(user(String.valueOf(OWNER_ID)).roles("OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startTime": "%s", "endTime": "%s"}
                                """.formatted(tenAm, elevenAm)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.propertyId").value(publishedProperty.getId()));

        // 2. Adjacent slot 11:00-12:00 by assigned AGENT -> 201 Created
        mvc.perform(post(url)
                        .with(csrf())
                        .with(user(String.valueOf(ASSIGNED_AGENT_ID)).roles("AGENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startTime": "%s", "endTime": "%s"}
                                """.formatted(elevenAm, twelvePm)))
                .andExpect(status().isCreated());

        // 3. Overlapping slot 10:30-11:30 -> 409 Conflict ProblemDetail
        mvc.perform(post(url)
                        .with(csrf())
                        .with(user(String.valueOf(OWNER_ID)).roles("OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startTime": "%s", "endTime": "%s"}
                                """.formatted(overlapStart, overlapEnd)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // 4. GET slots -> 200 OK with the 2 non-overlapping slots
        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Non-owner / unassigned agent attempting to create a slot on another user's property receives 403 Forbidden")
    void createSlot_byNonOwner_returns403() throws Exception {
        Instant start = Instant.now().plus(4, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(1, ChronoUnit.HOURS);
        String url = "/api/properties/" + publishedProperty.getId() + "/slots";

        mvc.perform(post(url)
                        .with(csrf())
                        .with(user(String.valueOf(OTHER_OWNER_ID)).roles("OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startTime": "%s", "endTime": "%s"}
                                """.formatted(start, end)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Invalid time range (endTime <= startTime) returns 400 Bad Request ProblemDetail")
    void createSlot_invalidRange_returns400() throws Exception {
        Instant start = Instant.now().plus(4, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        String url = "/api/properties/" + publishedProperty.getId() + "/slots";

        mvc.perform(post(url)
                        .with(csrf())
                        .with(user(String.valueOf(OWNER_ID)).roles("OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startTime": "%s", "endTime": "%s"}
                                """.formatted(start, start)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("DELETE slot by owner returns 204 No Content and removes slot")
    void deleteSlot_byOwner_returns204() throws Exception {
        Instant start = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(1, ChronoUnit.HOURS);
        String baseUrl = "/api/properties/" + publishedProperty.getId() + "/slots";

        String responseJson = mvc.perform(post(baseUrl)
                        .with(csrf())
                        .with(user(String.valueOf(OWNER_ID)).roles("OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startTime": "%s", "endTime": "%s"}
                                """.formatted(start, end)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Extract created id and delete it
        String idStr = responseJson.replaceAll(".*\"id\"\\s*:\\s*(\\d+).*", "$1");

        mvc.perform(delete(baseUrl + "/" + idStr)
                        .with(csrf())
                        .with(user(String.valueOf(OTHER_OWNER_ID)).roles("OWNER")))
                .andExpect(status().isForbidden());

        mvc.perform(delete(baseUrl + "/" + idStr)
                        .with(csrf())
                        .with(user(String.valueOf(OWNER_ID)).roles("OWNER")))
                .andExpect(status().isNoContent());
    }
}
