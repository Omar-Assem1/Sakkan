package com.sakan.viewing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.PayloadApplicationEvent;

import com.sakan.common.domain.Currency;
import com.sakan.common.event.StatusChangedEvent;
import com.sakan.common.exception.InvalidStatusTransitionException;
import com.sakan.common.security.AuthenticatedUser;
import com.sakan.common.statemachine.StatusTransitionService;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;
import com.sakan.property.dto.PropertyResponse;
import com.sakan.property.service.PropertyService;
import com.sakan.viewing.domain.Viewing;
import com.sakan.viewing.domain.ViewingSlot;
import com.sakan.viewing.dto.RequestViewingRequest;
import com.sakan.viewing.repository.ViewingRepository;
import com.sakan.viewing.repository.ViewingSlotRepository;

import static org.mockito.Mockito.mock;

class ViewingStatusChangeEventTest {
    @Test
    void applicationListenerReceivesRequestConfirmCancelAndCompleteEvents() {
        SimpleApplicationEventMulticaster eventBus = new SimpleApplicationEventMulticaster();
        List<StatusChangedEvent> received = new ArrayList<>();
        eventBus.addApplicationListener(
                (ApplicationListener<PayloadApplicationEvent<StatusChangedEvent>>) event -> received.add(event.getPayload()));

        ViewingConflictChecker conflictChecker = mock(ViewingConflictChecker.class);
        ViewingRepository viewings = mock(ViewingRepository.class);
        ViewingSlotRepository slots = mock(ViewingSlotRepository.class);
        PropertyService properties = mock(PropertyService.class);
        ViewingSlot slot = new ViewingSlot();
        slot.setId(50L);
        slot.setPropertyId(100L);
        when(conflictChecker.assertSlotBookable(anyLong(), any())).thenReturn(slot);
        when(slots.findById(50L)).thenReturn(Optional.of(slot));
        when(properties.getById(eq(100L), org.mockito.ArgumentMatchers.<AuthenticatedUser>any()))
                .thenReturn(publishedProperty());

        AtomicLong ids = new AtomicLong(76L);
        Map<Long, Viewing> persisted = new java.util.HashMap<>();
        when(viewings.saveAndFlush(any(Viewing.class))).thenAnswer(invocation -> {
            Viewing viewing = invocation.getArgument(0);
            viewing.setId(ids.incrementAndGet());
            persisted.put(viewing.getId(), viewing);
            return viewing;
        });
        when(viewings.findById(anyLong())).thenAnswer(invocation -> Optional.ofNullable(persisted.get(invocation.getArgument(0))));
        when(viewings.save(any(Viewing.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ViewingService service = new ViewingServiceImpl(conflictChecker, viewings, slots,
                Mappers.getMapper(ViewingMapper.class), properties,
                new StatusTransitionService(event -> eventBus.multicastEvent(
                        new PayloadApplicationEvent<>(this, event))));
        AuthenticatedUser buyer = new AuthenticatedUser(30L, Set.of("BUYER_TENANT"));
        AuthenticatedUser owner = new AuthenticatedUser(10L, Set.of(AuthenticatedUser.OWNER));

        var requestedForCompletion = service.requestViewing(new RequestViewingRequest(50L, null), buyer);
        service.confirmViewing(requestedForCompletion.id(), owner);
        service.completeViewing(requestedForCompletion.id(), owner);

        var requestedForCancellation = service.requestViewing(new RequestViewingRequest(50L, null), buyer);
        service.cancelViewing(requestedForCancellation.id(), buyer);
        assertThatThrownBy(() -> service.completeViewing(requestedForCancellation.id(), owner))
                .isInstanceOf(InvalidStatusTransitionException.class);

        assertThat(received).extracting(StatusChangedEvent::newStatus)
                .containsExactly("REQUESTED", "CONFIRMED", "COMPLETED", "REQUESTED", "CANCELLED");
        assertThat(received).extracting(StatusChangedEvent::previousStatus)
                .containsExactly(null, "REQUESTED", "CONFIRMED", null, "REQUESTED");
        assertThat(received).allSatisfy(event -> {
            assertThat(event.entityType()).isEqualTo("Viewing");
            assertThat(event.entityId()).isNotBlank();
            assertThat(event.occurredAt()).isNotNull();
        });
        assertThat(received.get(0).actor()).isEqualTo("user:30");
        assertThat(received.get(1).actor()).isEqualTo("user:10");
    }

    private static PropertyResponse publishedProperty() {
        return new PropertyResponse(100L, 10L, null, "Test property", "Description", PropertyPurpose.SALE,
                1L, 5L, new BigDecimal("100000.00"), Currency.EGP, 2, 1, new BigDecimal("80.00"),
                Set.of(), PropertyStatus.PUBLISHED, Instant.now(), Instant.now());
    }
}
