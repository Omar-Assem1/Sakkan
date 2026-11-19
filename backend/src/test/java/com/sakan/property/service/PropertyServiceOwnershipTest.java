package com.sakan.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sakan.common.exception.ForbiddenOperationException;
import com.sakan.common.exception.ResourceNotFoundException;
import com.sakan.common.security.AuthenticatedUser;
import com.sakan.property.domain.Property;
import com.sakan.property.domain.PropertyStatus;
import com.sakan.property.dto.PropertyMapper;
import com.sakan.property.repository.PropertyRepository;
import com.sakan.support.tags.UnitTest;

/** SCRUM-65 / NFR-03: ownership and role rules live in the service layer. */
@UnitTest
@ExtendWith(MockitoExtension.class)
class PropertyServiceOwnershipTest {

    private static final long PROPERTY_ID = 10L;
    private static final long OWNER_ID = 1L;
    private static final long AGENT_ID = 2L;
    private static final long OTHER_ID = 3L;
    private static final long ADMIN_ID = 4L;
    private static final long BUYER_ID = 5L;

    @Mock private PropertyRepository repository;
    @Mock private PropertyMapper mapper;

    private PropertyService service;

    @BeforeEach
    void setUp() {
        service = new PropertyService(repository, mapper);
    }

    private static AuthenticatedUser user(long id, String... roles) {
        return new AuthenticatedUser(id, Set.of(roles));
    }

    private Property givenProperty(PropertyStatus status) {
        Property p = new Property();
        p.setOwnerId(OWNER_ID);
        p.setAgentId(AGENT_ID);
        p.setStatus(status);
        when(repository.findById(PROPERTY_ID)).thenReturn(Optional.of(p));
        return p;
    }

    // ---------- create / listMine ----------

    @Test
    @DisplayName("create: BUYER_TENANT and ADMIN are forbidden")
    void create_nonListingRoles_forbidden() {
        assertThatThrownBy(() -> service.create(null, user(BUYER_ID, "BUYER_TENANT")))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.create(null, user(ADMIN_ID, "ADMIN")))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("create: the caller becomes the owner, the listing starts as DRAFT")
    void create_owner_setsOwnerAndDraft() {
        Property entity = new Property();
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(any(Property.class))).thenAnswer(i -> i.getArgument(0));

        service.create(null, user(OWNER_ID, "OWNER"));

        assertThat(entity.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(entity.getAgentId()).isNull();
        assertThat(entity.getStatus()).isEqualTo(PropertyStatus.DRAFT);
    }

    @Test
    @DisplayName("create: an agent is stored as owner and assigned agent")
    void create_agent_setsAgentId() {
        Property entity = new Property();
        when(mapper.toEntity(any())).thenReturn(entity);
        when(repository.save(any(Property.class))).thenAnswer(i -> i.getArgument(0));

        service.create(null, user(AGENT_ID, "AGENT"));

        assertThat(entity.getAgentId()).isEqualTo(AGENT_ID);
    }

    @Test
    @DisplayName("listMine: BUYER_TENANT and ADMIN are forbidden")
    void listMine_nonListingRoles_forbidden() {
        assertThatThrownBy(() -> service.listMine(user(BUYER_ID, "BUYER_TENANT"), 0, 10))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.listMine(user(ADMIN_ID, "ADMIN"), 0, 10))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    // ---------- update / publish / archive ----------

    @Test
    @DisplayName("update: another owner is forbidden and nothing is saved")
    void update_otherOwner_forbidden() {
        givenProperty(PropertyStatus.DRAFT);
        assertThatThrownBy(() -> service.update(PROPERTY_ID, null, user(OTHER_ID, "OWNER")))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update: ADMIN and BUYER_TENANT are forbidden (admin moderation is out of scope)")
    void update_adminAndBuyer_forbidden() {
        givenProperty(PropertyStatus.DRAFT);
        assertThatThrownBy(() -> service.update(PROPERTY_ID, null, user(ADMIN_ID, "ADMIN")))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.update(PROPERTY_ID, null, user(BUYER_ID, "BUYER_TENANT")))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("update: the owner and the assigned agent may edit")
    void update_ownerAndAssignedAgent_allowed() {
        Property p = givenProperty(PropertyStatus.DRAFT);
        when(repository.save(any(Property.class))).thenAnswer(i -> i.getArgument(0));

        assertThatCode(() -> service.update(PROPERTY_ID, null, user(OWNER_ID, "OWNER"))).doesNotThrowAnyException();
        assertThatCode(() -> service.update(PROPERTY_ID, null, user(AGENT_ID, "AGENT"))).doesNotThrowAnyException();
        verify(repository, org.mockito.Mockito.times(2)).save(p);
    }

    @Test
    @DisplayName("publish: another owner is forbidden")
    void publish_otherOwner_forbidden() {
        givenProperty(PropertyStatus.DRAFT);
        assertThatThrownBy(() -> service.publish(PROPERTY_ID, user(OTHER_ID, "OWNER")))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("publish: the owner may publish a DRAFT")
    void publish_owner_allowed() {
        Property p = givenProperty(PropertyStatus.DRAFT);
        when(repository.save(any(Property.class))).thenAnswer(i -> i.getArgument(0));

        service.publish(PROPERTY_ID, user(OWNER_ID, "OWNER"));

        assertThat(p.getStatus()).isEqualTo(PropertyStatus.PUBLISHED);
    }

    @Test
    @DisplayName("archive: another agent and ADMIN are forbidden")
    void archive_otherAgentAndAdmin_forbidden() {
        givenProperty(PropertyStatus.PUBLISHED);
        assertThatThrownBy(() -> service.archive(PROPERTY_ID, user(OTHER_ID, "AGENT")))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.archive(PROPERTY_ID, user(ADMIN_ID, "ADMIN")))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update of a missing property is 404, not 403")
    void update_missingProperty_notFound() {
        when(repository.findById(PROPERTY_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(PROPERTY_ID, null, user(OWNER_ID, "OWNER")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- visibility (getById) ----------

    @Test
    @DisplayName("getById: a DRAFT is hidden (404) from anonymous callers, buyers and other owners")
    void getById_draft_hiddenFromOutsiders() {
        givenProperty(PropertyStatus.DRAFT);
        assertThatThrownBy(() -> service.getById(PROPERTY_ID, null)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.getById(PROPERTY_ID, user(BUYER_ID, "BUYER_TENANT")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.getById(PROPERTY_ID, user(OTHER_ID, "OWNER")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getById: a DRAFT is visible to its owner, its agent and ADMIN")
    void getById_draft_visibleToManagersAndAdmin() {
        givenProperty(PropertyStatus.DRAFT);
        assertThatCode(() -> service.getById(PROPERTY_ID, user(OWNER_ID, "OWNER"))).doesNotThrowAnyException();
        assertThatCode(() -> service.getById(PROPERTY_ID, user(AGENT_ID, "AGENT"))).doesNotThrowAnyException();
        assertThatCode(() -> service.getById(PROPERTY_ID, user(ADMIN_ID, "ADMIN"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getById: a PUBLISHED property is visible to anonymous callers")
    void getById_published_publicVisible() {
        givenProperty(PropertyStatus.PUBLISHED);
        assertThatCode(() -> service.getById(PROPERTY_ID, null)).doesNotThrowAnyException();
    }
}
