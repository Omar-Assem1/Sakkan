package com.sakan.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.sakan.common.domain.Currency;
import com.sakan.common.exception.ForbiddenOperationException;
import com.sakan.common.exception.InvalidStatusTransitionException;
import com.sakan.common.exception.ResourceNotFoundException;
import com.sakan.common.response.PageResponse;
import com.sakan.common.security.AuthenticatedUser;
import com.sakan.common.statemachine.StatusTransitionService;
import com.sakan.location.repository.LocationRepository;
import com.sakan.property.domain.Property;
import com.sakan.property.domain.PropertyPurpose;
import com.sakan.property.domain.PropertyStatus;
import com.sakan.property.dto.PropertyMapper;
import com.sakan.property.dto.PropertyRequest;
import com.sakan.property.dto.PropertyResponse;
import com.sakan.property.repository.PropertyRepository;

import com.sakan.support.tags.RegressionTest;

@RegressionTest
@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    private static final long OWNER_ID = 1L;
    private static final long OTHER_ID = 2L;
    private static final long PROPERTY_ID = 10L;

    private static final AuthenticatedUser OWNER = new AuthenticatedUser(OWNER_ID, Set.of(AuthenticatedUser.OWNER));
    private static final AuthenticatedUser AGENT = new AuthenticatedUser(OTHER_ID, Set.of(AuthenticatedUser.AGENT));
    private static final AuthenticatedUser OTHER_OWNER = new AuthenticatedUser(OTHER_ID, Set.of(AuthenticatedUser.OWNER));
    private static final AuthenticatedUser BUYER = new AuthenticatedUser(3L, Set.of("BUYER_TENANT"));
    private static final AuthenticatedUser ADMIN = new AuthenticatedUser(4L, Set.of(AuthenticatedUser.ADMIN));

    @Mock
    private PropertyRepository repository;

    private PropertyService service;

    @BeforeEach
    void setUp() {
        service = new PropertyService(repository, Mappers.getMapper(PropertyMapper.class),
                new DefaultLocationHierarchyService(mock(LocationRepository.class)), new PropertySortValidator(),
                new StatusTransitionService(event -> { }));
    }

    @Test
    void create_asOwner_savesDraftOwnedByCaller() {
        when(repository.save(any(Property.class))).thenAnswer(inv -> inv.getArgument(0));

        PropertyResponse response = service.create(request("Sea view flat"), OWNER);

        assertThat(response.status()).isEqualTo(PropertyStatus.DRAFT);
        assertThat(response.ownerId()).isEqualTo(OWNER_ID);
        assertThat(response.agentId()).isNull();
        assertThat(response.title()).isEqualTo("Sea view flat");
        assertThat(response.price()).isEqualByComparingTo("2500000");
        assertThat(response.currency()).isEqualTo(Currency.EGP);
        assertThat(response.amenityIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void create_asAgent_alsoSetsAssignedAgent() {
        when(repository.save(any(Property.class))).thenAnswer(inv -> inv.getArgument(0));

        PropertyResponse response = service.create(request("Villa"), AGENT);

        assertThat(response.ownerId()).isEqualTo(OTHER_ID);
        assertThat(response.agentId()).isEqualTo(OTHER_ID);
    }

    @Test
    void create_asBuyer_isForbidden() {
        assertThatThrownBy(() -> service.create(request("Flat"), BUYER))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void update_byNonOwner_isRejectedAndNothingIsSaved() {
        when(repository.findById(PROPERTY_ID)).thenReturn(Optional.of(property(PropertyStatus.DRAFT)));

        assertThatThrownBy(() -> service.update(PROPERTY_ID, request("Hijacked"), OTHER_OWNER))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void update_byAdmin_isRejected() {
        when(repository.findById(PROPERTY_ID)).thenReturn(Optional.of(property(PropertyStatus.DRAFT)));

        assertThatThrownBy(() -> service.update(PROPERTY_ID, request("Admin edit"), ADMIN))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void update_byOwner_changesEditableFieldsButNotOwnerOrStatus() {
        Property existing = property(PropertyStatus.PUBLISHED);
        when(repository.findById(PROPERTY_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(Property.class))).thenAnswer(inv -> inv.getArgument(0));

        PropertyResponse response = service.update(PROPERTY_ID, request("New title"), OWNER);

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.ownerId()).isEqualTo(OWNER_ID);
        assertThat(response.status()).isEqualTo(PropertyStatus.PUBLISHED);
    }

    @Test
    void update_byAssignedAgent_isAllowed() {
        Property existing = property(PropertyStatus.DRAFT);
        existing.setAgentId(OTHER_ID);
        when(repository.findById(PROPERTY_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(Property.class))).thenAnswer(inv -> inv.getArgument(0));

        PropertyResponse response = service.update(PROPERTY_ID, request("Agent edit"), AGENT);

        assertThat(response.title()).isEqualTo("Agent edit");
    }

    @Test
    void update_ofArchivedListing_isRejected() {
        when(repository.findById(PROPERTY_ID)).thenReturn(Optional.of(property(PropertyStatus.ARCHIVED)));

        assertThatThrownBy(() -> service.update(PROPERTY_ID, request("Too late"), OWNER))
                .isInstanceOf(InvalidStatusTransitionException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void update_ofMissingListing_isNotFound() {
        when(repository.findById(PROPERTY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(PROPERTY_ID, request("x"), OWNER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void publish_draft_becomesPublished() {
        when(repository.findById(PROPERTY_ID)).thenReturn(Optional.of(property(PropertyStatus.DRAFT)));
        when(repository.save(any(Property.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.publish(PROPERTY_ID, OWNER).status()).isEqualTo(PropertyStatus.PUBLISHED);
    }

    @Test
    void publish_archivedListing_isIllegalTransition() {
        when(repository.findById(PROPERTY_ID)).thenReturn(Optional.of(property(PropertyStatus.ARCHIVED)));

        assertThatThrownBy(() -> service.publish(PROPERTY_ID, OWNER))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessageContaining("ARCHIVED")
                .hasMessageContaining("PUBLISHED");

        verify(repository, never()).save(any());
    }

    @Test
    void publish_alreadyPublished_isIllegalTransition() {
        when(repository.findById(PROPERTY_ID)).thenReturn(Optional.of(property(PropertyStatus.PUBLISHED)));

        assertThatThrownBy(() -> service.publish(PROPERTY_ID, OWNER))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void publish_byNonOwner_isForbidden() {
        when(repository.findById(PROPERTY_ID)).thenReturn(Optional.of(property(PropertyStatus.DRAFT)));

        assertThatThrownBy(() -> service.publish(PROPERTY_ID, OTHER_OWNER))
                .isInstanceOf(ForbiddenOperationException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void archive_published_becomesArchived() {
        when(repository.findById(PROPERTY_ID)).thenReturn(Optional.of(property(PropertyStatus.PUBLISHED)));
        when(repository.save(any(Property.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.archive(PROPERTY_ID, OWNER).status()).isEqualTo(PropertyStatus.ARCHIVED);
    }

    @Test
    void archive_alreadyArchived_isIllegalTransition() {
        when(repository.findById(PROPERTY_ID)).thenReturn(Optional.of(property(PropertyStatus.ARCHIVED)));

        assertThatThrownBy(() -> service.archive(PROPERTY_ID, OWNER))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void getById_published_isVisibleToAnonymous() {
        when(repository.findById(PROPERTY_ID)).thenReturn(Optional.of(property(PropertyStatus.PUBLISHED)));

        assertThat(service.getById(PROPERTY_ID, null).id()).isEqualTo(PROPERTY_ID);
    }

    @Test
    void getById_draft_isHiddenFromAnonymousAndOtherUsers() {
        when(repository.findById(PROPERTY_ID)).thenReturn(Optional.of(property(PropertyStatus.DRAFT)));

        assertThatThrownBy(() -> service.getById(PROPERTY_ID, null))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.getById(PROPERTY_ID, OTHER_OWNER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_draft_isVisibleToOwnerAndAdmin() {
        when(repository.findById(PROPERTY_ID)).thenReturn(Optional.of(property(PropertyStatus.DRAFT)));

        assertThat(service.getById(PROPERTY_ID, OWNER).status()).isEqualTo(PropertyStatus.DRAFT);
        assertThat(service.getById(PROPERTY_ID, ADMIN).status()).isEqualTo(PropertyStatus.DRAFT);
    }

    @Test
    void listMine_returnsCallersListingsAsPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findManagedBy(eq(OWNER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(property(PropertyStatus.DRAFT)), pageable, 1));

        PageResponse<PropertyResponse> page = service.listMine(OWNER, 0, 20);

        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.page()).isZero();
    }

    @Test
    void listMine_capsPageSizeAndFixesNegativePage() {
        when(repository.findManagedBy(eq(OWNER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listMine(OWNER, -5, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findManagedBy(eq(OWNER_ID), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isZero();
        assertThat(captor.getValue().getPageSize()).isEqualTo(PropertyService.MAX_PAGE_SIZE);
    }

    @Test
    void listMine_withNonPositiveSize_usesDefault() {
        when(repository.findManagedBy(eq(OWNER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.listMine(OWNER, 0, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findManagedBy(eq(OWNER_ID), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(PropertyService.DEFAULT_PAGE_SIZE);
    }

    @Test
    void listMine_asBuyer_isForbidden() {
        assertThatThrownBy(() -> service.listMine(BUYER, 0, 20))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    private static PropertyRequest request(String title) {
        return new PropertyRequest(
                title, "Bright, 3 minutes from the sea", PropertyPurpose.SALE, 1L, 5L,
                new BigDecimal("2500000"), Currency.EGP, 3, 2, new BigDecimal("140.50"), Set.of(1L, 2L));
    }

    private static Property property(PropertyStatus status) {
        Property p = new Property();
        ReflectionTestUtils.setField(p, "id", PROPERTY_ID);
        p.setOwnerId(OWNER_ID);
        p.setTitle("Existing listing");
        p.setPurpose(PropertyPurpose.SALE);
        p.setPropertyTypeId(1L);
        p.setLocationId(5L);
        p.setPrice(new BigDecimal("1000000"));
        p.setCurrency(Currency.EGP);
        p.setBedrooms(2);
        p.setBathrooms(1);
        p.setAreaSqm(new BigDecimal("90.00"));
        p.setStatus(status);
        return p;
    }
}
