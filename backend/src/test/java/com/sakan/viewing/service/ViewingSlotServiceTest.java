package com.sakan.viewing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sakan.common.domain.Currency;
import com.sakan.common.exception.ForbiddenOperationException;
import com.sakan.common.exception.InvalidStatusTransitionException;
import com.sakan.common.exception.ResourceNotFoundException;
import com.sakan.common.exception.SlotConflictException;
import com.sakan.common.security.AuthenticatedUser;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;
import com.sakan.property.dto.PropertyResponse;
import com.sakan.property.service.PropertyService;
import com.sakan.support.tags.UnitTest;
import com.sakan.viewing.dto.CreateViewingSlotRequest;
import com.sakan.viewing.dto.ViewingSlotResponse;
import com.sakan.viewing.entity.ViewingSlot;
import com.sakan.viewing.repository.ViewingSlotRepository;

@UnitTest
@ExtendWith(MockitoExtension.class)
class ViewingSlotServiceTest {

    private static final long PROPERTY_ID = 100L;
    private static final long OWNER_ID = 10L;
    private static final long ASSIGNED_AGENT_ID = 20L;
    private static final long OTHER_OWNER_ID = 99L;

    private static final AuthenticatedUser OWNER =
            new AuthenticatedUser(OWNER_ID, Set.of(AuthenticatedUser.OWNER));
    private static final AuthenticatedUser ASSIGNED_AGENT =
            new AuthenticatedUser(ASSIGNED_AGENT_ID, Set.of(AuthenticatedUser.AGENT));
    private static final AuthenticatedUser OTHER_OWNER =
            new AuthenticatedUser(OTHER_OWNER_ID, Set.of(AuthenticatedUser.OWNER));
    private static final AuthenticatedUser BUYER =
            new AuthenticatedUser(30L, Set.of("BUYER_TENANT"));

    @Mock
    private ViewingSlotRepository slotRepository;

    @Mock
    private PropertyService propertyService;

    private ViewingSlotService service;

    @BeforeEach
    void setUp() {
        ViewingSlotMapper mapper = Mappers.getMapper(ViewingSlotMapper.class);
        service = new ViewingSlotServiceImpl(slotRepository, propertyService, mapper);
    }

    @Test
    @DisplayName("Property OWNER can create a viewing slot on their PUBLISHED property")
    void createSlot_byOwnerOnPublishedProperty_succeeds() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        when(propertyService.getById(PROPERTY_ID, OWNER))
                .thenReturn(propertyResponse(PropertyStatus.PUBLISHED));
        when(slotRepository.existsOverlappingSlot(PROPERTY_ID, start, end)).thenReturn(false);
        when(slotRepository.save(any(ViewingSlot.class))).thenAnswer(inv -> {
            ViewingSlot slot = inv.getArgument(0);
            slot.setId(1L);
            return slot;
        });

        ViewingSlotResponse result =
                service.createSlot(PROPERTY_ID, new CreateViewingSlotRequest(start, end), OWNER);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.propertyId()).isEqualTo(PROPERTY_ID);
        assertThat(result.createdBy()).isEqualTo(OWNER_ID);
        assertThat(result.startTime()).isEqualTo(start);
        assertThat(result.endTime()).isEqualTo(end);
    }

    @Test
    @DisplayName("Assigned AGENT can create a viewing slot on their assigned PUBLISHED property")
    void createSlot_byAssignedAgentOnPublishedProperty_succeeds() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        when(propertyService.getById(PROPERTY_ID, ASSIGNED_AGENT))
                .thenReturn(propertyResponse(PropertyStatus.PUBLISHED));
        when(slotRepository.existsOverlappingSlot(PROPERTY_ID, start, end)).thenReturn(false);
        when(slotRepository.save(any(ViewingSlot.class))).thenAnswer(inv -> {
            ViewingSlot slot = inv.getArgument(0);
            slot.setId(2L);
            return slot;
        });

        ViewingSlotResponse result = service.createSlot(
                PROPERTY_ID, new CreateViewingSlotRequest(start, end), ASSIGNED_AGENT);

        assertThat(result.id()).isEqualTo(2L);
        assertThat(result.createdBy()).isEqualTo(ASSIGNED_AGENT_ID);
    }

    @Test
    @DisplayName("Adjacent non-overlapping slots (10:00-11:00 and 11:00-12:00) both succeed")
    void createSlot_adjacentNonOverlappingSlots_succeed() {
        Instant tenAm = Instant.now().plus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant elevenAm = tenAm.plus(1, ChronoUnit.HOURS);
        Instant twelvePm = tenAm.plus(2, ChronoUnit.HOURS);

        when(propertyService.getById(PROPERTY_ID, OWNER))
                .thenReturn(propertyResponse(PropertyStatus.PUBLISHED));
        when(slotRepository.existsOverlappingSlot(PROPERTY_ID, tenAm, elevenAm)).thenReturn(false);
        when(slotRepository.existsOverlappingSlot(PROPERTY_ID, elevenAm, twelvePm)).thenReturn(false);
        when(slotRepository.save(any(ViewingSlot.class))).thenAnswer(inv -> inv.getArgument(0));

        ViewingSlotResponse first =
                service.createSlot(PROPERTY_ID, new CreateViewingSlotRequest(tenAm, elevenAm), OWNER);
        ViewingSlotResponse second =
                service.createSlot(PROPERTY_ID, new CreateViewingSlotRequest(elevenAm, twelvePm), OWNER);

        assertThat(first.endTime()).isEqualTo(second.startTime());
    }

    @Test
    @DisplayName("Overlapping slot on the same property is rejected with SlotConflictException (409)")
    void createSlot_overlappingSlot_throwsSlotConflictException() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        when(propertyService.getById(PROPERTY_ID, OWNER))
                .thenReturn(propertyResponse(PropertyStatus.PUBLISHED));
        when(slotRepository.existsOverlappingSlot(PROPERTY_ID, start, end)).thenReturn(true);

        assertThatThrownBy(() ->
                service.createSlot(PROPERTY_ID, new CreateViewingSlotRequest(start, end), OWNER))
                .isInstanceOf(SlotConflictException.class)
                .hasMessageContaining("overlaps");

        verify(slotRepository, never()).save(any());
    }

    @Test
    @DisplayName("Caller who is neither property owner nor assigned agent receives ForbiddenOperationException (403)")
    void createSlot_byNonOwnerOrUnassignedAgent_throwsForbiddenOperationException() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        when(propertyService.getById(PROPERTY_ID, OTHER_OWNER))
                .thenReturn(propertyResponse(PropertyStatus.PUBLISHED));

        assertThatThrownBy(() ->
                service.createSlot(PROPERTY_ID, new CreateViewingSlotRequest(start, end), OTHER_OWNER))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(slotRepository, never()).save(any());
    }

    @Test
    @DisplayName("BUYER_TENANT attempting to create a slot receives ForbiddenOperationException (403)")
    void createSlot_byBuyerTenant_throwsForbiddenOperationException() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        assertThatThrownBy(() ->
                service.createSlot(PROPERTY_ID, new CreateViewingSlotRequest(start, end), BUYER))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(slotRepository, never()).save(any());
    }

    @Test
    @DisplayName("Invalid time range (endTime <= startTime) throws IllegalArgumentException (400)")
    void createSlot_endTimeBeforeOrEqualStartTime_throwsIllegalArgumentException() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant sameTime = start;
        Instant earlierTime = start.minus(30, ChronoUnit.MINUTES);

        assertThatThrownBy(() ->
                service.createSlot(PROPERTY_ID, new CreateViewingSlotRequest(start, sameTime), OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endTime");

        assertThatThrownBy(() ->
                service.createSlot(PROPERTY_ID, new CreateViewingSlotRequest(start, earlierTime), OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endTime");
    }

    @Test
    @DisplayName("Creating a slot on a non-PUBLISHED (DRAFT or ARCHIVED) property is rejected (409)")
    void createSlot_onNonPublishedProperty_throwsInvalidStatusTransitionException() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant end = start.plus(1, ChronoUnit.HOURS);

        when(propertyService.getById(PROPERTY_ID, OWNER))
                .thenReturn(propertyResponse(PropertyStatus.DRAFT));

        assertThatThrownBy(() ->
                service.createSlot(PROPERTY_ID, new CreateViewingSlotRequest(start, end), OWNER))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("PUBLISHED");

        verify(slotRepository, never()).save(any());
    }

    @Test
    @DisplayName("listSlotsForProperty returns slots ordered by startTime ascending")
    void listSlotsForProperty_returnsOrderedSlots() {
        Instant start1 = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant start2 = Instant.now().plus(2, ChronoUnit.DAYS);

        ViewingSlot slot1 = new ViewingSlot();
        slot1.setId(10L);
        slot1.setPropertyId(PROPERTY_ID);
        slot1.setCreatedBy(OWNER_ID);
        slot1.setStartTime(start1);
        slot1.setEndTime(start1.plus(1, ChronoUnit.HOURS));

        ViewingSlot slot2 = new ViewingSlot();
        slot2.setId(11L);
        slot2.setPropertyId(PROPERTY_ID);
        slot2.setCreatedBy(OWNER_ID);
        slot2.setStartTime(start2);
        slot2.setEndTime(start2.plus(1, ChronoUnit.HOURS));

        when(propertyService.getById(eq(PROPERTY_ID), any()))
                .thenReturn(propertyResponse(PropertyStatus.PUBLISHED));
        when(slotRepository.findByPropertyIdOrderByStartTimeAsc(PROPERTY_ID))
                .thenReturn(List.of(slot1, slot2));

        List<ViewingSlotResponse> results = service.listSlotsForProperty(PROPERTY_ID, null);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).id()).isEqualTo(10L);
        assertThat(results.get(1).id()).isEqualTo(11L);
    }

    @Test
    @DisplayName("deleteSlot removes slot when called by property owner and rejects non-owner with 403")
    void deleteSlot_byOwnerSucceeds_andByOtherOwnerThrows403() {
        ViewingSlot slot = new ViewingSlot();
        slot.setId(55L);
        slot.setPropertyId(PROPERTY_ID);
        slot.setCreatedBy(OWNER_ID);

        when(propertyService.getById(PROPERTY_ID, OWNER))
                .thenReturn(propertyResponse(PropertyStatus.PUBLISHED));
        when(slotRepository.findByIdAndPropertyId(55L, PROPERTY_ID))
                .thenReturn(Optional.of(slot));

        service.deleteSlot(PROPERTY_ID, 55L, OWNER);
        verify(slotRepository).delete(slot);

        when(propertyService.getById(PROPERTY_ID, OTHER_OWNER))
                .thenReturn(propertyResponse(PropertyStatus.PUBLISHED));

        assertThatThrownBy(() -> service.deleteSlot(PROPERTY_ID, 55L, OTHER_OWNER))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("deleteSlot throws ResourceNotFoundException (404) when slot does not exist on property")
    void deleteSlot_missingSlot_throwsResourceNotFoundException() {
        when(propertyService.getById(PROPERTY_ID, OWNER))
                .thenReturn(propertyResponse(PropertyStatus.PUBLISHED));
        when(slotRepository.findByIdAndPropertyId(404L, PROPERTY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteSlot(PROPERTY_ID, 404L, OWNER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static PropertyResponse propertyResponse(PropertyStatus status) {
        return new PropertyResponse(
                PROPERTY_ID,
                OWNER_ID,
                ASSIGNED_AGENT_ID,
                "Alexandria Sea View Apartment",
                "Spacious 3-bedroom apartment",
                PropertyPurpose.SALE,
                1L,
                5L,
                new BigDecimal("3500000.00"),
                Currency.EGP,
                3,
                2,
                new BigDecimal("160.00"),
                Set.of(1L, 2L),
                status,
                Instant.now(),
                Instant.now());
    }
}
