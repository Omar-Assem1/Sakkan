package com.sakan.viewing.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.sakan.common.domain.Currency;
import com.sakan.property.domain.Property;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;
import com.sakan.property.repository.PropertyRepository;
import com.sakan.support.AbstractIntegrationTest;
import com.sakan.support.security.EndpointSecurityAssertions;
import com.sakan.support.tags.IntegrationTest;
import com.sakan.support.tags.RegressionTest;
import com.sakan.viewing.domain.Viewing;
import com.sakan.viewing.domain.ViewingStatus;
import com.sakan.viewing.entity.ViewingSlot;
import com.sakan.viewing.repository.ViewingRepository;
import com.sakan.viewing.repository.ViewingSlotRepository;

/**
 * Concurrency, row-locking, re-booking, self-booking, and security/validation integration tests
 * for SCRUM-98 / SCRUM-101 (POST /api/viewings).
 *
 * <p>Intentionally NOT annotated with {@code @Transactional}: worker threads in the concurrency
 * test must see committed {@code Property} and {@code ViewingSlot} rows in PostgreSQL.
 */
@RegressionTest
@IntegrationTest
class ViewingConcurrencyIT extends AbstractIntegrationTest {

    private static final String VIEWINGS_URL = "/api/viewings";
    private static final long OWNER_ID = 201L;
    private static final long ASSIGNED_AGENT_ID = 202L;
    private static final long BUYER_1_ID = 301L;
    private static final long BUYER_2_ID = 302L;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private ViewingSlotRepository viewingSlotRepository;

    @Autowired
    private ViewingRepository viewingRepository;

    private MockMvc mvc;
    private Property publishedProperty;
    private ViewingSlot slot;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        Property p = new Property();
        p.setOwnerId(OWNER_ID);
        p.setAgentId(ASSIGNED_AGENT_ID);
        p.setTitle("Stanley Bay Viewing Concurrency Flat");
        p.setDescription("Published property for SCRUM-101 concurrency IT");
        p.setPurpose(PropertyPurpose.SALE);
        p.setPropertyTypeId(1L);
        p.setLocationId(1L);
        p.setPrice(new BigDecimal("4200000.00"));
        p.setCurrency(Currency.EGP);
        p.setBedrooms(3);
        p.setBathrooms(2);
        p.setAreaSqm(new BigDecimal("175.00"));
        p.setStatus(PropertyStatus.PUBLISHED);
        publishedProperty = propertyRepository.saveAndFlush(p);

        Instant start = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        ViewingSlot s = new ViewingSlot();
        s.setPropertyId(publishedProperty.getId());
        s.setCreatedBy(OWNER_ID);
        s.setStartTime(start);
        s.setEndTime(start.plus(1, ChronoUnit.HOURS));
        slot = viewingSlotRepository.saveAndFlush(s);
    }

    @AfterEach
    void tearDown() {
        if (slot != null && slot.getId() != null) {
            List<Viewing> slotViewings = viewingRepository.findAll().stream()
                    .filter(v -> slot.getId().equals(v.getSlotId()))
                    .toList();
            viewingRepository.deleteAll(slotViewings);
            viewingSlotRepository.deleteById(slot.getId());
        }
        if (publishedProperty != null && publishedProperty.getId() != null) {
            propertyRepository.deleteById(publishedProperty.getId());
        }
    }

    @Test
    @DisplayName("Concurrent race: 2 simultaneous buyers booking the same slot -> exactly one 201 Created, one 409 Conflict ProblemDetail, and 1 active row")
    void concurrentBooking_sameSlot_exactlyOne201AndOne409() throws Exception {
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        String payload = """
                {"slotId": %d, "notes": "Concurrent booking request"}
                """.formatted(slot.getId());

        Callable<MvcResult> buyer1Task = () -> {
            readyLatch.countDown();
            assertThat(startLatch.await(5, TimeUnit.SECONDS)).isTrue();
            return mvc.perform(post(VIEWINGS_URL)
                            .with(csrf())
                            .with(user(String.valueOf(BUYER_1_ID)).roles("BUYER_TENANT"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andReturn();
        };

        Callable<MvcResult> buyer2Task = () -> {
            readyLatch.countDown();
            assertThat(startLatch.await(5, TimeUnit.SECONDS)).isTrue();
            return mvc.perform(post(VIEWINGS_URL)
                            .with(csrf())
                            .with(user(String.valueOf(BUYER_2_ID)).roles("BUYER_TENANT"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andReturn();
        };

        try {
            Future<MvcResult> first = executor.submit(buyer1Task);
            Future<MvcResult> second = executor.submit(buyer2Task);

            assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
            startLatch.countDown();

            List<MvcResult> results = List.of(
                    first.get(15, TimeUnit.SECONDS),
                    second.get(15, TimeUnit.SECONDS));

            List<Integer> statuses = results.stream()
                    .map(r -> r.getResponse().getStatus())
                    .toList();
            assertThat(statuses).containsExactlyInAnyOrder(201, 409);

            MvcResult conflictResult = results.stream()
                    .filter(r -> r.getResponse().getStatus() == 409)
                    .findFirst()
                    .orElseThrow();
            String conflictBody = conflictResult.getResponse().getContentAsString();
            assertThat((Integer) JsonPath.read(conflictBody, "$.status")).isEqualTo(409);
            assertThat((String) JsonPath.read(conflictBody, "$.title")).isNotBlank();

            long activeCount = viewingRepository.countBySlotIdAndStatusIn(
                    slot.getId(), EnumSet.of(ViewingStatus.REQUESTED, ViewingStatus.CONFIRMED));
            assertThat(activeCount).isEqualTo(1L);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("Re-booking after cancellation: slot with only a CANCELLED viewing succeeds with 201 Created")
    void rebookingAfterCancellation_returns201Created() throws Exception {
        Viewing cancelled = new Viewing();
        cancelled.setSlotId(slot.getId());
        cancelled.setPropertyId(publishedProperty.getId());
        cancelled.setBuyerId(BUYER_1_ID);
        cancelled.setStatus(ViewingStatus.CANCELLED);
        cancelled.setNotes("Previously cancelled");
        viewingRepository.saveAndFlush(cancelled);

        mvc.perform(post(VIEWINGS_URL)
                        .with(csrf())
                        .with(user(String.valueOf(BUYER_2_ID)).roles("BUYER_TENANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slotId": %d, "notes": "Re-booking cancelled slot"}
                                """.formatted(slot.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slotId").value(slot.getId()))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    @DisplayName("Self-booking rejection: property OWNER and assigned AGENT cannot book their own property's slot (403 Forbidden)")
    void selfBooking_byOwnerOrAssignedAgent_returns403ProblemDetail() throws Exception {
        String payload = """
                {"slotId": %d, "notes": "Self-booking attempt"}
                """.formatted(slot.getId());

        mvc.perform(post(VIEWINGS_URL)
                        .with(csrf())
                        .with(user(String.valueOf(OWNER_ID)).roles("OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        mvc.perform(post(VIEWINGS_URL)
                        .with(csrf())
                        .with(user(String.valueOf(ASSIGNED_AGENT_ID)).roles("AGENT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("Security & Validation: 401 Unauthorized without JWT, 400 Bad Request for null slotId or oversized notes")
    void securityAndValidation_unauthenticatedReturns401_andInvalidPayloadReturns400() throws Exception {
        EndpointSecurityAssertions.assertUnauthorized(mvc, HttpMethod.POST, VIEWINGS_URL);

        // Null slotId -> 400 Bad Request ProblemDetail
        mvc.perform(post(VIEWINGS_URL)
                        .with(csrf())
                        .with(user(String.valueOf(BUYER_1_ID)).roles("BUYER_TENANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"notes": "Missing slotId"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        // Oversized notes -> 400 Bad Request ProblemDetail
        String oversizedNotes = "x".repeat(2001);
        mvc.perform(post(VIEWINGS_URL)
                        .with(csrf())
                        .with(user(String.valueOf(BUYER_1_ID)).roles("BUYER_TENANT"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"slotId": %d, "notes": "%s"}
                                """.formatted(slot.getId(), oversizedNotes)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
