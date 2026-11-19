package com.sakan.viewing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sakan.common.domain.Currency;
import com.sakan.common.exception.ForbiddenOperationException;
import com.sakan.common.exception.ResourceNotFoundException;
import com.sakan.common.exception.SlotConflictException;
import com.sakan.common.security.AuthenticatedUser;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;
import com.sakan.property.dto.PropertyResponse;
import com.sakan.property.service.PropertyService;
import com.sakan.support.tags.RegressionTest;
import com.sakan.support.tags.UnitTest;
import com.sakan.viewing.domain.ViewingStatus;
import com.sakan.viewing.domain.ViewingSlot;
import com.sakan.viewing.repository.ViewingRepository;
import com.sakan.viewing.repository.ViewingSlotRepository;

@RegressionTest
@UnitTest
@ExtendWith(MockitoExtension.class)
class ViewingConflictCheckerTest {

    private static final long PROPERTY_ID = 100L;
    private static final long SLOT_ID = 50L;
    private static final long OWNER_ID = 10L;
    private static final long ASSIGNED_AGENT_ID = 20L;
    private static final long BUYER_ID = 30L;

    private static final AuthenticatedUser OWNER =
            new AuthenticatedUser(OWNER_ID, Set.of(AuthenticatedUser.OWNER));
    private static final AuthenticatedUser ASSIGNED_AGENT =
            new AuthenticatedUser(ASSIGNED_AGENT_ID, Set.of(AuthenticatedUser.AGENT));
    private static final AuthenticatedUser BUYER =
            new AuthenticatedUser(BUYER_ID, Set.of("BUYER_TENANT"));

    @Mock
    private ViewingSlotRepository slotRepository;

    @Mock
    private ViewingRepository viewingRepository;

    @Mock
    private PropertyService propertyService;

    private ViewingConflictChecker checker;

    @BeforeEach
    void setUp() {
        checker = new ViewingConflictChecker(slotRepository, viewingRepository, propertyService);
    }

    @Test
    @DisplayName("Available slot with no active viewing passes conflict check")
    void assertSlotBookable_availableSlotWithNoActiveViewing_passes() {
        ViewingSlot slot = futureSlot();
        when(slotRepository.findByIdForUpdate(SLOT_ID)).thenReturn(Optional.of(slot));
        when(propertyService.getById(PROPERTY_ID, BUYER)).thenReturn(publishedProperty());
        when(viewingRepository.existsBySlotIdAndStatusIn(eq(SLOT_ID), any())).thenReturn(false);

        ViewingSlot result = checker.assertSlotBookable(SLOT_ID, BUYER);

        assertThat(result.getId()).isEqualTo(SLOT_ID);
        verify(slotRepository).findByIdForUpdate(SLOT_ID);
    }

    @Test
    @DisplayName("Slot with an existing REQUESTED or CONFIRMED viewing throws SlotConflictException (409)")
    void assertSlotBookable_slotWithActiveViewing_throwsSlotConflictException() {
        ViewingSlot slot = futureSlot();
        when(slotRepository.findByIdForUpdate(SLOT_ID)).thenReturn(Optional.of(slot));
        when(propertyService.getById(PROPERTY_ID, BUYER)).thenReturn(publishedProperty());
        when(viewingRepository.existsBySlotIdAndStatusIn(eq(SLOT_ID), any())).thenReturn(true);

        assertThatThrownBy(() -> checker.assertSlotBookable(SLOT_ID, BUYER))
                .isInstanceOf(SlotConflictException.class);
    }

    @Test
    @DisplayName("Slot with only a CANCELLED viewing passes conflict check (re-booking allowed)")
    void assertSlotBookable_slotWithOnlyCancelledViewing_passes() {
        ViewingSlot slot = futureSlot();
        when(slotRepository.findByIdForUpdate(SLOT_ID)).thenReturn(Optional.of(slot));
        when(propertyService.getById(PROPERTY_ID, BUYER)).thenReturn(publishedProperty());
        when(viewingRepository.existsBySlotIdAndStatusIn(
                eq(SLOT_ID),
                argThat(statuses -> statuses != null
                        && statuses.contains(ViewingStatus.REQUESTED)
                        && statuses.contains(ViewingStatus.CONFIRMED)
                        && !statuses.contains(ViewingStatus.CANCELLED))))
                .thenReturn(false);

        ViewingSlot result = checker.assertSlotBookable(SLOT_ID, BUYER);

        assertThat(result.getId()).isEqualTo(SLOT_ID);
    }

    @Test
    @DisplayName("Property OWNER trying to book their own property's slot is rejected (403)")
    void assertSlotBookable_ownerBookingOwnProperty_throwsForbiddenOperationException() {
        ViewingSlot slot = futureSlot();
        when(slotRepository.findByIdForUpdate(SLOT_ID)).thenReturn(Optional.of(slot));
        when(propertyService.getById(PROPERTY_ID, OWNER)).thenReturn(publishedProperty());

        assertThatThrownBy(() -> checker.assertSlotBookable(SLOT_ID, OWNER))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(viewingRepository, never()).existsBySlotIdAndStatusIn(any(), any());
    }

    @Test
    @DisplayName("Assigned AGENT trying to book their own assigned property's slot is rejected (403)")
    void assertSlotBookable_assignedAgentBookingOwnProperty_throwsForbiddenOperationException() {
        ViewingSlot slot = futureSlot();
        when(slotRepository.findByIdForUpdate(SLOT_ID)).thenReturn(Optional.of(slot));
        when(propertyService.getById(PROPERTY_ID, ASSIGNED_AGENT)).thenReturn(publishedProperty());

        assertThatThrownBy(() -> checker.assertSlotBookable(SLOT_ID, ASSIGNED_AGENT))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(viewingRepository, never()).existsBySlotIdAndStatusIn(any(), any());
    }

    @Test
    @DisplayName("Non-existent slotId throws ResourceNotFoundException (404)")
    void assertSlotBookable_missingSlot_throwsResourceNotFoundException() {
        long missingSlotId = 404L;
        when(slotRepository.findByIdForUpdate(missingSlotId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> checker.assertSlotBookable(missingSlotId, BUYER))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(viewingRepository, never()).existsBySlotIdAndStatusIn(any(), any());
    }

    private static ViewingSlot futureSlot() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        ViewingSlot slot = new ViewingSlot();
        slot.setId(SLOT_ID);
        slot.setPropertyId(PROPERTY_ID);
        slot.setCreatedBy(OWNER_ID);
        slot.setStartTime(start);
        slot.setEndTime(start.plus(1, ChronoUnit.HOURS));
        return slot;
    }

    private static PropertyResponse publishedProperty() {
        return new PropertyResponse(
                PROPERTY_ID,
                OWNER_ID,
                ASSIGNED_AGENT_ID,
                "Smouha Garden Apartment",
                "Published test property",
                PropertyPurpose.SALE,
                1L,
                5L,
                new BigDecimal("3500000.00"),
                Currency.EGP,
                3,
                2,
                new BigDecimal("160.00"),
                Set.of(1L),
                PropertyStatus.PUBLISHED,
                Instant.now(),
                Instant.now());
    }
}
