package com.sakan.viewing.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
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

import com.sakan.common.exception.ForbiddenOperationException;
import com.sakan.common.exception.GlobalExceptionHandler;
import com.sakan.common.exception.InvalidStatusTransitionException;
import com.sakan.common.exception.SlotConflictException;
import com.sakan.common.security.AuthenticatedUser;
import com.sakan.common.security.CurrentUserProvider;
import com.sakan.support.tags.UnitTest;
import com.sakan.viewing.dto.CreateViewingSlotRequest;
import com.sakan.viewing.dto.ViewingSlotResponse;
import com.sakan.viewing.service.ViewingSlotService;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ViewingSlotControllerTest {

    private static final long PROPERTY_ID = 10L;
    private static final AuthenticatedUser OWNER =
            new AuthenticatedUser(1L, Set.of(AuthenticatedUser.OWNER));

    @Mock
    private ViewingSlotService viewingSlotService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ViewingSlotController(viewingSlotService, currentUserProvider))
                .setValidator(validator)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/properties/{propertyId}/slots returns 201 Created with Location header")
    void createSlot_validRequest_returns201Created() throws Exception {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant end = start.plus(1, ChronoUnit.HOURS);
        ViewingSlotResponse response = new ViewingSlotResponse(77L, PROPERTY_ID, 1L, start, end, Instant.now());

        when(currentUserProvider.require()).thenReturn(OWNER);
        when(viewingSlotService.createSlot(eq(PROPERTY_ID), any(CreateViewingSlotRequest.class), eq(OWNER)))
                .thenReturn(response);

        String body = """
                {
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(start, end);

        mvc.perform(post("/api/properties/{propertyId}/slots", PROPERTY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/properties/10/slots/77"))
                .andExpect(jsonPath("$.id").value(77))
                .andExpect(jsonPath("$.propertyId").value(10));
    }

    @Test
    @DisplayName("POST overlapping slot returns 409 Conflict ProblemDetail")
    void createSlot_overlapping_returns409ProblemDetail() throws Exception {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        when(currentUserProvider.require()).thenReturn(OWNER);
        when(viewingSlotService.createSlot(eq(PROPERTY_ID), any(CreateViewingSlotRequest.class), eq(OWNER)))
                .thenThrow(new SlotConflictException("Viewing slot overlaps with an existing slot on property 10"));

        String body = """
                {
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(start, end);

        mvc.perform(post("/api/properties/{propertyId}/slots", PROPERTY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    @DisplayName("POST by non-owner/non-assigned-agent returns 403 Forbidden ProblemDetail")
    void createSlot_nonManager_returns403ProblemDetail() throws Exception {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        when(currentUserProvider.require()).thenReturn(OWNER);
        when(viewingSlotService.createSlot(eq(PROPERTY_ID), any(CreateViewingSlotRequest.class), eq(OWNER)))
                .thenThrow(new ForbiddenOperationException("Only the property owner or assigned agent can manage viewing slots"));

        String body = """
                {
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(start, end);

        mvc.perform(post("/api/properties/{propertyId}/slots", PROPERTY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    @DisplayName("POST with past startTime or missing fields returns 400 Bad Request ProblemDetail")
    void createSlot_pastTimestampOrNull_returns400ProblemDetail() throws Exception {
        Instant pastStart = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);

        String body = """
                {
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(pastStart, end);

        mvc.perform(post("/api/properties/{propertyId}/slots", PROPERTY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST on non-PUBLISHED property returns 409 Conflict ProblemDetail")
    void createSlot_nonPublishedProperty_returns409ProblemDetail() throws Exception {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        when(currentUserProvider.require()).thenReturn(OWNER);
        when(viewingSlotService.createSlot(eq(PROPERTY_ID), any(CreateViewingSlotRequest.class), eq(OWNER)))
                .thenThrow(new InvalidStatusTransitionException("Viewing slots can only be managed for PUBLISHED properties"));

        String body = """
                {
                  "startTime": "%s",
                  "endTime": "%s"
                }
                """.formatted(start, end);

        mvc.perform(post("/api/properties/{propertyId}/slots", PROPERTY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET /api/properties/{propertyId}/slots returns 200 OK with slot list")
    void listSlots_returns200Ok() throws Exception {
        Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        Instant end = start.plus(1, ChronoUnit.HOURS);
        ViewingSlotResponse slot = new ViewingSlotResponse(5L, PROPERTY_ID, 1L, start, end, Instant.now());

        when(currentUserProvider.find()).thenReturn(Optional.empty());
        when(viewingSlotService.listSlotsForProperty(PROPERTY_ID, null)).thenReturn(List.of(slot));

        mvc.perform(get("/api/properties/{propertyId}/slots", PROPERTY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].propertyId").value(10));
    }

    @Test
    @DisplayName("DELETE /api/properties/{propertyId}/slots/{slotId} returns 204 No Content for owner and 403 for non-owner")
    void deleteSlot_returns204Or403() throws Exception {
        when(currentUserProvider.require()).thenReturn(OWNER);

        mvc.perform(delete("/api/properties/{propertyId}/slots/{slotId}", PROPERTY_ID, 5L))
                .andExpect(status().isNoContent());

        verify(viewingSlotService).deleteSlot(PROPERTY_ID, 5L, OWNER);

        doThrow(new ForbiddenOperationException("Forbidden"))
                .when(viewingSlotService).deleteSlot(PROPERTY_ID, 99L, OWNER);

        mvc.perform(delete("/api/properties/{propertyId}/slots/{slotId}", PROPERTY_ID, 99L))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
