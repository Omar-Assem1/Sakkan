package com.sakan.viewing.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.jdbc.core.JdbcTemplate;
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
import com.sakan.support.TestUserFixtures;
import com.sakan.support.security.EndpointSecurityAssertions;
import com.sakan.support.tags.IntegrationTest;
import com.sakan.support.tags.RegressionTest;
import com.sakan.viewing.domain.Viewing;
import com.sakan.viewing.domain.ViewingStatus;
import com.sakan.viewing.domain.ViewingSlot;
import com.sakan.viewing.repository.ViewingRepository;
import com.sakan.viewing.repository.ViewingSlotRepository;

/**
 * Integration & security tests for SCRUM-102 / SCRUM-105:
 * PATCH /api/viewings/{id}/{confirm,cancel,complete} and GET /api/viewings/my.
 */
@RegressionTest
@IntegrationTest
@Transactional
class ViewingStatusControllerIT extends AbstractIntegrationTest {

    private static final String VIEWINGS_BASE = "/api/viewings";
    private static final long OWNER_ID = 401L;
    private static final long ASSIGNED_AGENT_ID = 402L;
    private static final long OTHER_OWNER_ID = 403L;
    private static final long BUYER_1_ID = 501L;
    private static final long BUYER_2_ID = 502L;
    private static final long UNRELATED_USER_ID = 599L;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private ViewingSlotRepository viewingSlotRepository;

    @Autowired
    private ViewingRepository viewingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mvc;
    private Property ownerProperty;
    private Property otherProperty;
    private ViewingSlot ownerSlot1;
    private ViewingSlot ownerSlot2;
    private ViewingSlot otherSlot;

    @BeforeEach
    void setUp() {
        TestUserFixtures.ensureUsers(jdbcTemplate, OWNER_ID, ASSIGNED_AGENT_ID, OTHER_OWNER_ID,
                BUYER_1_ID, BUYER_2_ID, UNRELATED_USER_ID);

        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        ownerProperty = savePublishedProperty(OWNER_ID, ASSIGNED_AGENT_ID, "Roushdy Corner Apartment");
        otherProperty = savePublishedProperty(OTHER_OWNER_ID, null, "Kafr Abdo Villa");

        Instant base = Instant.now().plus(5, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        ownerSlot1 = saveSlot(ownerProperty.getId(), OWNER_ID, base, base.plus(1, ChronoUnit.HOURS));
        ownerSlot2 = saveSlot(ownerProperty.getId(), OWNER_ID, base.plus(2, ChronoUnit.HOURS), base.plus(3, ChronoUnit.HOURS));
        otherSlot = saveSlot(otherProperty.getId(), OTHER_OWNER_ID, base, base.plus(1, ChronoUnit.HOURS));
    }

    @Test
    @DisplayName("Valid transitions: confirm (REQUESTED->CONFIRMED), complete (CONFIRMED->COMPLETED), and cancel (REQUESTED->CANCELLED) return 200 OK")
    void validTransitions_confirmCompleteAndCancel_return200WithUpdatedStatus() throws Exception {
        Viewing viewing1 = saveViewing(ownerSlot1.getId(), ownerProperty.getId(), BUYER_1_ID, ViewingStatus.REQUESTED);

        // OWNER confirms: REQUESTED -> CONFIRMED
        mvc.perform(patch(VIEWINGS_BASE + "/" + viewing1.getId() + "/confirm")
                        .with(csrf())
                        .with(user(String.valueOf(OWNER_ID)).roles("OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(viewing1.getId()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        // Assigned AGENT completes: CONFIRMED -> COMPLETED
        mvc.perform(patch(VIEWINGS_BASE + "/" + viewing1.getId() + "/complete")
                        .with(csrf())
                        .with(user(String.valueOf(ASSIGNED_AGENT_ID)).roles("AGENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(viewing1.getId()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Requesting BUYER cancels: REQUESTED -> CANCELLED
        Viewing viewing2 = saveViewing(ownerSlot2.getId(), ownerProperty.getId(), BUYER_2_ID, ViewingStatus.REQUESTED);
        mvc.perform(patch(VIEWINGS_BASE + "/" + viewing2.getId() + "/cancel")
                        .with(csrf())
                        .with(user(String.valueOf(BUYER_2_ID)).roles("BUYER_TENANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(viewing2.getId()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("Invalid transitions (REQUESTED->COMPLETED and COMPLETED->CANCELLED) return 409 Conflict ProblemDetail")
    void invalidTransitions_return409ConflictProblemDetail() throws Exception {
        Viewing requested = saveViewing(ownerSlot1.getId(), ownerProperty.getId(), BUYER_1_ID, ViewingStatus.REQUESTED);
        mvc.perform(patch(VIEWINGS_BASE + "/" + requested.getId() + "/complete")
                        .with(csrf())
                        .with(user(String.valueOf(OWNER_ID)).roles("OWNER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        Viewing completed = saveViewing(ownerSlot2.getId(), ownerProperty.getId(), BUYER_2_ID, ViewingStatus.COMPLETED);
        mvc.perform(patch(VIEWINGS_BASE + "/" + completed.getId() + "/cancel")
                        .with(csrf())
                        .with(user(String.valueOf(OWNER_ID)).roles("OWNER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("403 Forbidden when BUYER_TENANT calls confirm/complete or unrelated user calls cancel")
    void authorization_buyerCannotConfirmOrComplete_andUnrelatedCannotCancel() throws Exception {
        Viewing requested = saveViewing(ownerSlot1.getId(), ownerProperty.getId(), BUYER_1_ID, ViewingStatus.REQUESTED);

        mvc.perform(patch(VIEWINGS_BASE + "/" + requested.getId() + "/confirm")
                        .with(csrf())
                        .with(user(String.valueOf(BUYER_1_ID)).roles("BUYER_TENANT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        Viewing confirmed = saveViewing(ownerSlot2.getId(), ownerProperty.getId(), BUYER_1_ID, ViewingStatus.CONFIRMED);
        mvc.perform(patch(VIEWINGS_BASE + "/" + confirmed.getId() + "/complete")
                        .with(csrf())
                        .with(user(String.valueOf(BUYER_1_ID)).roles("BUYER_TENANT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mvc.perform(patch(VIEWINGS_BASE + "/" + requested.getId() + "/cancel")
                        .with(csrf())
                        .with(user(String.valueOf(UNRELATED_USER_ID)).roles("BUYER_TENANT")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("401 Unauthorized when calling PATCH transitions or GET /api/viewings/my without JWT")
    void security_unauthenticatedRequestsReturn401() throws Exception {
        Viewing viewing = saveViewing(ownerSlot1.getId(), ownerProperty.getId(), BUYER_1_ID, ViewingStatus.REQUESTED);

        EndpointSecurityAssertions.assertUnauthorized(mvc, HttpMethod.PATCH, VIEWINGS_BASE + "/" + viewing.getId() + "/confirm");
        EndpointSecurityAssertions.assertUnauthorized(mvc, HttpMethod.PATCH, VIEWINGS_BASE + "/" + viewing.getId() + "/cancel");
        EndpointSecurityAssertions.assertUnauthorized(mvc, HttpMethod.PATCH, VIEWINGS_BASE + "/" + viewing.getId() + "/complete");
        EndpointSecurityAssertions.assertUnauthorized(mvc, HttpMethod.GET, VIEWINGS_BASE + "/my");
    }

    @Test
    @DisplayName("GET /api/viewings/my returns paginated viewings filtered by caller role and optional status query param")
    void myViewings_filteredByRoleAndOptionalStatus() throws Exception {
        // Property 1 (OWNER_ID / ASSIGNED_AGENT_ID): 1 REQUESTED by BUYER_1, 1 CONFIRMED by BUYER_2
        saveViewing(ownerSlot1.getId(), ownerProperty.getId(), BUYER_1_ID, ViewingStatus.REQUESTED);
        saveViewing(ownerSlot2.getId(), ownerProperty.getId(), BUYER_2_ID, ViewingStatus.CONFIRMED);
        // Property 2 (OTHER_OWNER_ID): 1 REQUESTED by BUYER_1
        saveViewing(otherSlot.getId(), otherProperty.getId(), BUYER_1_ID, ViewingStatus.REQUESTED);

        // BUYER_1 sees only their 2 requested viewings (across both properties), never BUYER_2's
        mvc.perform(get(VIEWINGS_BASE + "/my")
                        .with(user(String.valueOf(BUYER_1_ID)).roles("BUYER_TENANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));

        // OWNER_ID sees only the 2 viewings on ownerProperty, never otherProperty's viewing
        mvc.perform(get(VIEWINGS_BASE + "/my")
                        .with(user(String.valueOf(OWNER_ID)).roles("OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));

        // OWNER_ID with ?status=CONFIRMED sees only the 1 CONFIRMED viewing on ownerProperty with slot window and property title
        mvc.perform(get(VIEWINGS_BASE + "/my")
                        .param("status", "CONFIRMED")
                        .with(user(String.valueOf(OWNER_ID)).roles("OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"))
                .andExpect(jsonPath("$.content[0].propertyTitle").value("Roushdy Corner Apartment"))
                .andExpect(jsonPath("$.content[0].slotStartTime").isNotEmpty())
                .andExpect(jsonPath("$.content[0].slotEndTime").isNotEmpty());

        // ASSIGNED_AGENT_ID sees the 2 viewings on assigned ownerProperty
        mvc.perform(get(VIEWINGS_BASE + "/my")
                        .with(user(String.valueOf(ASSIGNED_AGENT_ID)).roles("AGENT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    private Property savePublishedProperty(Long ownerId, Long agentId, String title) {
        Property p = new Property();
        p.setOwnerId(ownerId);
        p.setAgentId(agentId);
        p.setTitle(title);
        p.setDescription("Published property for SCRUM-105 IT");
        p.setPurpose(PropertyPurpose.SALE);
        p.setPropertyTypeId(1L);
        p.setLocationId(1L);
        p.setPrice(new BigDecimal("3500000.00"));
        p.setCurrency(Currency.EGP);
        p.setBedrooms(3);
        p.setBathrooms(2);
        p.setAreaSqm(new BigDecimal("160.00"));
        p.setStatus(PropertyStatus.PUBLISHED);
        return propertyRepository.saveAndFlush(p);
    }

    private ViewingSlot saveSlot(Long propertyId, Long createdBy, Instant start, Instant end) {
        ViewingSlot slot = new ViewingSlot();
        slot.setPropertyId(propertyId);
        slot.setCreatedBy(createdBy);
        slot.setStartTime(start);
        slot.setEndTime(end);
        return viewingSlotRepository.saveAndFlush(slot);
    }

    private Viewing saveViewing(Long slotId, Long propertyId, Long buyerId, ViewingStatus status) {
        Viewing viewing = new Viewing();
        viewing.setSlotId(slotId);
        viewing.setPropertyId(propertyId);
        viewing.setBuyerId(buyerId);
        viewing.setStatus(status);
        viewing.setNotes("Seeded viewing");
        return viewingRepository.saveAndFlush(viewing);
    }
}
