package com.sakan.viewing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
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
import com.sakan.common.security.AuthenticatedUser;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;
import com.sakan.property.dto.PropertyResponse;
import com.sakan.property.service.PropertyService;
import com.sakan.support.tags.RegressionTest;
import com.sakan.support.tags.UnitTest;
import com.sakan.viewing.domain.Viewing;
import com.sakan.viewing.domain.ViewingStatus;
import com.sakan.viewing.dto.ViewingResponse;
import com.sakan.viewing.repository.ViewingRepository;

@RegressionTest
@UnitTest
@ExtendWith(MockitoExtension.class)
class ViewingStatusTransitionTest {

    private static final long VIEWING_ID = 77L;
    private static final long SLOT_ID = 50L;
    private static final long PROPERTY_ID = 100L;
    private static final long OWNER_ID = 10L;
    private static final long ASSIGNED_AGENT_ID = 20L;
    private static final long BUYER_ID = 30L;
    private static final long UNRELATED_USER_ID = 99L;

    private static final AuthenticatedUser OWNER =
            new AuthenticatedUser(OWNER_ID, Set.of(AuthenticatedUser.OWNER));
    private static final AuthenticatedUser ASSIGNED_AGENT =
            new AuthenticatedUser(ASSIGNED_AGENT_ID, Set.of(AuthenticatedUser.AGENT));
    private static final AuthenticatedUser BUYER =
            new AuthenticatedUser(BUYER_ID, Set.of("BUYER_TENANT"));
    private static final AuthenticatedUser UNRELATED_USER =
            new AuthenticatedUser(UNRELATED_USER_ID, Set.of("BUYER_TENANT"));

    @Mock
    private ViewingConflictChecker conflictChecker;

    @Mock
    private ViewingRepository viewingRepository;

    @Mock
    private PropertyService propertyService;

    private ViewingService service;

    @BeforeEach
    void setUp() {
        ViewingMapper mapper = Mappers.getMapper(ViewingMapper.class);
        service = new ViewingServiceImpl(conflictChecker, viewingRepository, mapper, propertyService);
    }

    // --- Valid transitions ---

    @Test
    @DisplayName("REQUESTED -> CONFIRMED succeeds for property OWNER and assigned AGENT")
    void confirmViewing_fromRequested_byOwnerOrAssignedAgent_succeeds() {
        Viewing ownerViewing = viewingWithStatus(ViewingStatus.REQUESTED);
        when(viewingRepository.findById(VIEWING_ID)).thenReturn(Optional.of(ownerViewing));
        when(propertyService.getById(PROPERTY_ID, OWNER)).thenReturn(publishedProperty());
        when(viewingRepository.save(any(Viewing.class))).thenAnswer(inv -> inv.getArgument(0));

        ViewingResponse ownerResult = service.confirmViewing(VIEWING_ID, OWNER);
        assertThat(ownerResult.status()).isEqualTo(ViewingStatus.CONFIRMED);

        Viewing agentViewing = viewingWithStatus(ViewingStatus.REQUESTED);
        when(viewingRepository.findById(VIEWING_ID)).thenReturn(Optional.of(agentViewing));
        when(propertyService.getById(PROPERTY_ID, ASSIGNED_AGENT)).thenReturn(publishedProperty());

        ViewingResponse agentResult = service.confirmViewing(VIEWING_ID, ASSIGNED_AGENT);
        assertThat(agentResult.status()).isEqualTo(ViewingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("REQUESTED -> CANCELLED succeeds for requesting BUYER_TENANT and property OWNER")
    void cancelViewing_fromRequested_byBuyerOrOwner_succeeds() {
        Viewing buyerViewing = viewingWithStatus(ViewingStatus.REQUESTED);
        when(viewingRepository.findById(VIEWING_ID)).thenReturn(Optional.of(buyerViewing));
        when(viewingRepository.save(any(Viewing.class))).thenAnswer(inv -> inv.getArgument(0));

        ViewingResponse buyerResult = service.cancelViewing(VIEWING_ID, BUYER);
        assertThat(buyerResult.status()).isEqualTo(ViewingStatus.CANCELLED);

        Viewing ownerViewing = viewingWithStatus(ViewingStatus.REQUESTED);
        when(viewingRepository.findById(VIEWING_ID)).thenReturn(Optional.of(ownerViewing));
        when(propertyService.getById(PROPERTY_ID, OWNER)).thenReturn(publishedProperty());

        ViewingResponse ownerResult = service.cancelViewing(VIEWING_ID, OWNER);
        assertThat(ownerResult.status()).isEqualTo(ViewingStatus.CANCELLED);
    }

    @Test
    @DisplayName("CONFIRMED -> CANCELLED succeeds for requesting BUYER_TENANT and assigned AGENT")
    void cancelViewing_fromConfirmed_byBuyerOrAssignedAgent_succeeds() {
        Viewing buyerViewing = viewingWithStatus(ViewingStatus.CONFIRMED);
        when(viewingRepository.findById(VIEWING_ID)).thenReturn(Optional.of(buyerViewing));
        when(viewingRepository.save(any(Viewing.class))).thenAnswer(inv -> inv.getArgument(0));

        ViewingResponse buyerResult = service.cancelViewing(VIEWING_ID, BUYER);
        assertThat(buyerResult.status()).isEqualTo(ViewingStatus.CANCELLED);

        Viewing agentViewing = viewingWithStatus(ViewingStatus.CONFIRMED);
        when(viewingRepository.findById(VIEWING_ID)).thenReturn(Optional.of(agentViewing));
        when(propertyService.getById(PROPERTY_ID, ASSIGNED_AGENT)).thenReturn(publishedProperty());

        ViewingResponse agentResult = service.cancelViewing(VIEWING_ID, ASSIGNED_AGENT);
        assertThat(agentResult.status()).isEqualTo(ViewingStatus.CANCELLED);
    }

    @Test
    @DisplayName("CONFIRMED -> COMPLETED succeeds for property OWNER and assigned AGENT")
    void completeViewing_fromConfirmed_byOwnerOrAssignedAgent_succeeds() {
        Viewing ownerViewing = viewingWithStatus(ViewingStatus.CONFIRMED);
        when(viewingRepository.findById(VIEWING_ID)).thenReturn(Optional.of(ownerViewing));
        when(propertyService.getById(PROPERTY_ID, OWNER)).thenReturn(publishedProperty());
        when(viewingRepository.save(any(Viewing.class))).thenAnswer(inv -> inv.getArgument(0));

        ViewingResponse ownerResult = service.completeViewing(VIEWING_ID, OWNER);
        assertThat(ownerResult.status()).isEqualTo(ViewingStatus.COMPLETED);

        Viewing agentViewing = viewingWithStatus(ViewingStatus.CONFIRMED);
        when(viewingRepository.findById(VIEWING_ID)).thenReturn(Optional.of(agentViewing));
        when(propertyService.getById(PROPERTY_ID, ASSIGNED_AGENT)).thenReturn(publishedProperty());

        ViewingResponse agentResult = service.completeViewing(VIEWING_ID, ASSIGNED_AGENT);
        assertThat(agentResult.status()).isEqualTo(ViewingStatus.COMPLETED);
    }

    // --- Invalid transitions ---

    @Test
    @DisplayName("COMPLETED -> CANCELLED is rejected with InvalidStatusTransitionException (409)")
    void cancelViewing_fromCompleted_throwsInvalidStatusTransitionException() {
        when(viewingRepository.findById(VIEWING_ID)).thenReturn(Optional.of(viewingWithStatus(ViewingStatus.COMPLETED)));
        when(propertyService.getById(PROPERTY_ID, OWNER)).thenReturn(publishedProperty());

        assertThatThrownBy(() -> service.cancelViewing(VIEWING_ID, OWNER))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verify(viewingRepository, never()).save(any());
    }

    @Test
    @DisplayName("CANCELLED -> CONFIRMED is rejected with InvalidStatusTransitionException (409)")
    void confirmViewing_fromCancelled_throwsInvalidStatusTransitionException() {
        when(viewingRepository.findById(VIEWING_ID)).thenReturn(Optional.of(viewingWithStatus(ViewingStatus.CANCELLED)));
        when(propertyService.getById(PROPERTY_ID, OWNER)).thenReturn(publishedProperty());

        assertThatThrownBy(() -> service.confirmViewing(VIEWING_ID, OWNER))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verify(viewingRepository, never()).save(any());
    }

    @Test
    @DisplayName("REQUESTED -> COMPLETED is rejected with InvalidStatusTransitionException (409)")
    void completeViewing_fromRequested_throwsInvalidStatusTransitionException() {
        when(viewingRepository.findById(VIEWING_ID)).thenReturn(Optional.of(viewingWithStatus(ViewingStatus.REQUESTED)));
        when(propertyService.getById(PROPERTY_ID, OWNER)).thenReturn(publishedProperty());

        assertThatThrownBy(() -> service.completeViewing(VIEWING_ID, OWNER))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verify(viewingRepository, never()).save(any());
    }

    // --- Authorization & ownership ---

    @Test
    @DisplayName("BUYER_TENANT cannot CONFIRM or COMPLETE a viewing (403 Forbidden)")
    void confirmOrComplete_byBuyerTenant_throwsForbiddenOperationException() {
        when(viewingRepository.findById(VIEWING_ID)).thenReturn(Optional.of(viewingWithStatus(ViewingStatus.REQUESTED)));
        when(propertyService.getById(PROPERTY_ID, BUYER)).thenReturn(publishedProperty());

        assertThatThrownBy(() -> service.confirmViewing(VIEWING_ID, BUYER))
                .isInstanceOf(ForbiddenOperationException.class);

        when(viewingRepository.findById(VIEWING_ID)).thenReturn(Optional.of(viewingWithStatus(ViewingStatus.CONFIRMED)));

        assertThatThrownBy(() -> service.completeViewing(VIEWING_ID, BUYER))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(viewingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Unrelated third-party user cannot mutate another user's viewing (403 Forbidden)")
    void cancelOrConfirm_byUnrelatedUser_throwsForbiddenOperationException() {
        when(viewingRepository.findById(VIEWING_ID)).thenReturn(Optional.of(viewingWithStatus(ViewingStatus.REQUESTED)));
        when(propertyService.getById(PROPERTY_ID, UNRELATED_USER)).thenReturn(publishedProperty());

        assertThatThrownBy(() -> service.cancelViewing(VIEWING_ID, UNRELATED_USER))
                .isInstanceOf(ForbiddenOperationException.class);

        assertThatThrownBy(() -> service.confirmViewing(VIEWING_ID, UNRELATED_USER))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(viewingRepository, never()).save(any());
    }

    // --- Missing resource ---

    @Test
    @DisplayName("Non-existent viewingId throws ResourceNotFoundException (404)")
    void transition_missingViewing_throwsResourceNotFoundException() {
        long missingId = 404L;
        when(viewingRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmViewing(missingId, OWNER))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.cancelViewing(missingId, BUYER))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.completeViewing(missingId, OWNER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static Viewing viewingWithStatus(ViewingStatus status) {
        Viewing viewing = new Viewing();
        viewing.setId(VIEWING_ID);
        viewing.setSlotId(SLOT_ID);
        viewing.setPropertyId(PROPERTY_ID);
        viewing.setBuyerId(BUYER_ID);
        viewing.setStatus(status);
        viewing.setNotes("Test viewing");
        viewing.setCreatedAt(Instant.now());
        viewing.setUpdatedAt(Instant.now());
        return viewing;
    }

    private static PropertyResponse publishedProperty() {
        return new PropertyResponse(
                PROPERTY_ID,
                OWNER_ID,
                ASSIGNED_AGENT_ID,
                "Gleem Bay Apartment",
                "Published test property",
                PropertyPurpose.SALE,
                1L,
                5L,
                new BigDecimal("3800000.00"),
                Currency.EGP,
                3,
                2,
                new BigDecimal("165.00"),
                Set.of(1L),
                PropertyStatus.PUBLISHED,
                Instant.now(),
                Instant.now());
    }
}
