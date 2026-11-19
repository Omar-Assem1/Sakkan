package com.sakan.viewing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.sakan.common.domain.Currency;
import com.sakan.common.exception.SlotConflictException;
import com.sakan.common.response.PageResponse;
import com.sakan.common.security.AuthenticatedUser;
import com.sakan.common.statemachine.StatusTransitionService;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;
import com.sakan.property.dto.PropertyResponse;
import com.sakan.property.service.PropertyService;
import com.sakan.support.tags.RegressionTest;
import com.sakan.support.tags.UnitTest;
import com.sakan.viewing.domain.Viewing;
import com.sakan.viewing.domain.ViewingSlot;
import com.sakan.viewing.domain.ViewingStatus;
import com.sakan.viewing.dto.RequestViewingRequest;
import com.sakan.viewing.dto.ViewingResponse;
import com.sakan.viewing.repository.ViewingRepository;
import com.sakan.viewing.repository.ViewingSlotRepository;

@RegressionTest
@UnitTest
@ExtendWith(MockitoExtension.class)
class ViewingServiceImplTest {

    private static final long SLOT_ID = 50L;
    private static final long PROPERTY_ID = 100L;
    private static final long OWNER_ID = 10L;
    private static final long BUYER_ID = 30L;
    private static final String PROPERTY_TITLE = "Smouha Sea View Flat";

    private static final AuthenticatedUser BUYER =
            new AuthenticatedUser(BUYER_ID, Set.of("BUYER_TENANT"));
    private static final AuthenticatedUser OWNER =
            new AuthenticatedUser(OWNER_ID, Set.of(AuthenticatedUser.OWNER));

    @Mock
    private ViewingConflictChecker conflictChecker;

    @Mock
    private ViewingRepository viewingRepository;

    @Mock
    private ViewingSlotRepository slotRepository;

    @Mock
    private PropertyService propertyService;

    private ViewingService service;

    @BeforeEach
    void setUp() {
        ViewingMapper mapper = Mappers.getMapper(ViewingMapper.class);
        service = new ViewingServiceImpl(conflictChecker, viewingRepository, slotRepository, mapper, propertyService,
                new StatusTransitionService(event -> { }));
    }

    @Test
    @DisplayName("requestViewing persists a REQUESTED viewing with slot window and propertyTitle")
    void requestViewing_validSlot_savesAndReturnsEnrichedResponse() {
        ViewingSlot slot = slot();
        when(conflictChecker.assertSlotBookable(SLOT_ID, BUYER)).thenReturn(slot);
        when(viewingRepository.saveAndFlush(any(Viewing.class))).thenAnswer(inv -> {
            Viewing v = inv.getArgument(0);
            v.setId(7L);
            return v;
        });
        when(propertyService.getById(PROPERTY_ID, BUYER)).thenReturn(propertyResponse());

        ViewingResponse response =
                service.requestViewing(new RequestViewingRequest(SLOT_ID, "Call before arriving"), BUYER);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.slotId()).isEqualTo(SLOT_ID);
        assertThat(response.propertyId()).isEqualTo(PROPERTY_ID);
        assertThat(response.buyerId()).isEqualTo(BUYER_ID);
        assertThat(response.status()).isEqualTo(ViewingStatus.REQUESTED);
        assertThat(response.notes()).isEqualTo("Call before arriving");
        assertThat(response.slotStartTime()).isEqualTo(slot.getStartTime());
        assertThat(response.slotEndTime()).isEqualTo(slot.getEndTime());
        assertThat(response.propertyTitle()).isEqualTo(PROPERTY_TITLE);
    }

    @Test
    @DisplayName("requestViewing translates partial-unique-index DataIntegrityViolationException into SlotConflictException (409)")
    void requestViewing_uniqueIndexViolation_throwsSlotConflictException() {
        when(conflictChecker.assertSlotBookable(SLOT_ID, BUYER)).thenReturn(slot());
        when(viewingRepository.saveAndFlush(any(Viewing.class)))
                .thenThrow(new DataIntegrityViolationException("uq_viewing_active_slot"));

        assertThatThrownBy(() -> service.requestViewing(new RequestViewingRequest(SLOT_ID, null), BUYER))
                .isInstanceOf(SlotConflictException.class)
                .hasMessageContaining(String.valueOf(SLOT_ID));
    }

    @Test
    @DisplayName("listMyViewings batch-loads slot windows and enriches propertyTitle for buyers")
    void listMyViewings_forBuyer_enrichesSlotWindowAndPropertyTitle() {
        ViewingSlot slot = slot();
        Viewing v = new Viewing();
        v.setId(7L);
        v.setSlotId(SLOT_ID);
        v.setPropertyId(PROPERTY_ID);
        v.setBuyerId(BUYER_ID);
        v.setStatus(ViewingStatus.REQUESTED);

        when(viewingRepository.findByBuyerId(eq(BUYER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(v)));
        when(slotRepository.findAllById(Set.of(SLOT_ID))).thenReturn(List.of(slot));
        when(propertyService.getById(PROPERTY_ID, BUYER)).thenReturn(propertyResponse());

        PageResponse<ViewingResponse> result = service.listMyViewings(BUYER, null, 0, 20);

        assertThat(result.content()).hasSize(1);
        ViewingResponse item = result.content().getFirst();
        assertThat(item.slotStartTime()).isEqualTo(slot.getStartTime());
        assertThat(item.slotEndTime()).isEqualTo(slot.getEndTime());
        assertThat(item.propertyTitle()).isEqualTo(PROPERTY_TITLE);
        verify(slotRepository).findAllById(Set.of(SLOT_ID));
    }

    @Test
    @DisplayName("listMyViewings returns empty page without querying viewingRepository when OWNER manages 0 properties")
    void listMyViewings_forOwnerWithNoProperties_returnsEmptyPage() {
        when(propertyService.listMine(OWNER, 0, 50))
                .thenReturn(new PageResponse<>(List.of(), 0, 50, 0, 0));

        PageResponse<ViewingResponse> result = service.listMyViewings(OWNER, null, 0, 20);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    private static ViewingSlot slot() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
        ViewingSlot slot = new ViewingSlot();
        slot.setId(SLOT_ID);
        slot.setPropertyId(PROPERTY_ID);
        slot.setCreatedBy(OWNER_ID);
        slot.setStartTime(start);
        slot.setEndTime(start.plus(1, ChronoUnit.HOURS));
        return slot;
    }

    private static PropertyResponse propertyResponse() {
        return new PropertyResponse(
                PROPERTY_ID,
                OWNER_ID,
                null,
                PROPERTY_TITLE,
                "Test description",
                PropertyPurpose.SALE,
                1L,
                1L,
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
