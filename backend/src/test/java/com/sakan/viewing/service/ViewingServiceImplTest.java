package com.sakan.viewing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.sakan.common.exception.SlotConflictException;
import com.sakan.common.security.AuthenticatedUser;
import com.sakan.support.tags.RegressionTest;
import com.sakan.support.tags.UnitTest;
import com.sakan.viewing.domain.Viewing;
import com.sakan.viewing.domain.ViewingStatus;
import com.sakan.viewing.dto.RequestViewingRequest;
import com.sakan.viewing.dto.ViewingResponse;
import com.sakan.viewing.entity.ViewingSlot;
import com.sakan.viewing.repository.ViewingRepository;

@RegressionTest
@UnitTest
@ExtendWith(MockitoExtension.class)
class ViewingServiceImplTest {

    private static final long SLOT_ID = 50L;
    private static final long PROPERTY_ID = 100L;
    private static final long BUYER_ID = 30L;
    private static final AuthenticatedUser BUYER =
            new AuthenticatedUser(BUYER_ID, Set.of("BUYER_TENANT"));

    @Mock
    private ViewingConflictChecker conflictChecker;

    @Mock
    private ViewingRepository viewingRepository;

    private ViewingService service;

    @BeforeEach
    void setUp() {
        ViewingMapper mapper = Mappers.getMapper(ViewingMapper.class);
        service = new ViewingServiceImpl(conflictChecker, viewingRepository, mapper);
    }

    @Test
    @DisplayName("requestViewing persists a REQUESTED viewing deriving propertyId from slot and buyerId from JWT")
    void requestViewing_validSlot_savesAndReturnsResponse() {
        ViewingSlot slot = slot();
        when(conflictChecker.assertSlotBookable(SLOT_ID, BUYER)).thenReturn(slot);
        when(viewingRepository.saveAndFlush(any(Viewing.class))).thenAnswer(inv -> {
            Viewing v = inv.getArgument(0);
            v.setId(7L);
            return v;
        });

        ViewingResponse response =
                service.requestViewing(new RequestViewingRequest(SLOT_ID, "Call before arriving"), BUYER);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.slotId()).isEqualTo(SLOT_ID);
        assertThat(response.propertyId()).isEqualTo(PROPERTY_ID);
        assertThat(response.buyerId()).isEqualTo(BUYER_ID);
        assertThat(response.status()).isEqualTo(ViewingStatus.REQUESTED);
        assertThat(response.notes()).isEqualTo("Call before arriving");
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

    private static ViewingSlot slot() {
        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        ViewingSlot slot = new ViewingSlot();
        slot.setId(SLOT_ID);
        slot.setPropertyId(PROPERTY_ID);
        slot.setCreatedBy(10L);
        slot.setStartTime(start);
        slot.setEndTime(start.plus(1, ChronoUnit.HOURS));
        return slot;
    }
}
