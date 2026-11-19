package com.sakan.property.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
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
import com.sakan.property.repository.PropertyImageRepository;
import com.sakan.property.repository.PropertyRepository;
import com.sakan.property.storage.ImageStorage;
import com.sakan.support.tags.UnitTest;

/** SCRUM-65: image management is owner/agent only, and image lists follow the property's visibility. */
@UnitTest
@ExtendWith(MockitoExtension.class)
class PropertyImageServiceAccessTest {

    private static final long PROPERTY_ID = 10L;
    private static final long OWNER_ID = 1L;
    private static final long AGENT_ID = 2L;
    private static final long OTHER_ID = 3L;
    private static final long ADMIN_ID = 4L;
    private static final long BUYER_ID = 5L;

    @Mock private PropertyRepository properties;
    @Mock private PropertyImageRepository images;
    @Mock private ImageStorage storage;

    private PropertyImageService service;

    @BeforeEach
    void setUp() {
        service = new PropertyImageService(properties, images, storage);
    }

    private static AuthenticatedUser user(long id, String... roles) {
        return new AuthenticatedUser(id, Set.of(roles));
    }

    private Property property(PropertyStatus status) {
        Property p = new Property();
        p.setOwnerId(OWNER_ID);
        p.setAgentId(AGENT_ID);
        p.setStatus(status);
        return p;
    }

    private void givenVisibleLookup(PropertyStatus status) {
        when(properties.findById(PROPERTY_ID)).thenReturn(Optional.of(property(status)));
    }

    // ---------- list: visibility follows the property (decision 9) ----------

    @Test
    @DisplayName("list: DRAFT property is hidden (404) from anonymous callers, buyers and other owners")
    void list_draft_hiddenFromOutsiders() {
        givenVisibleLookup(PropertyStatus.DRAFT);
        assertThatThrownBy(() -> service.list(PROPERTY_ID, null)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.list(PROPERTY_ID, user(BUYER_ID, "BUYER_TENANT")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.list(PROPERTY_ID, user(OTHER_ID, "OWNER")))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(images);
    }

    @Test
    @DisplayName("list: ARCHIVED property is hidden from the public")
    void list_archived_hiddenFromPublic() {
        givenVisibleLookup(PropertyStatus.ARCHIVED);
        assertThatThrownBy(() -> service.list(PROPERTY_ID, null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("list: DRAFT property is visible to its owner, its agent and ADMIN")
    void list_draft_visibleToManagersAndAdmin() {
        givenVisibleLookup(PropertyStatus.DRAFT);
        when(images.findByPropertyIdAndArchivedFalseOrderByDisplayOrderAsc(PROPERTY_ID)).thenReturn(List.of());

        assertThatCode(() -> service.list(PROPERTY_ID, user(OWNER_ID, "OWNER"))).doesNotThrowAnyException();
        assertThatCode(() -> service.list(PROPERTY_ID, user(AGENT_ID, "AGENT"))).doesNotThrowAnyException();
        assertThatCode(() -> service.list(PROPERTY_ID, user(ADMIN_ID, "ADMIN"))).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("list: PUBLISHED property is visible to anonymous callers")
    void list_published_publicVisible() {
        givenVisibleLookup(PropertyStatus.PUBLISHED);
        when(images.findByPropertyIdAndArchivedFalseOrderByDisplayOrderAsc(PROPERTY_ID)).thenReturn(List.of());

        assertThatCode(() -> service.list(PROPERTY_ID, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("list: unknown property is 404")
    void list_missingProperty_notFound() {
        when(properties.findById(PROPERTY_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.list(PROPERTY_ID, null)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- upload / reorder / delete: owner or assigned agent only ----------

    @Test
    @DisplayName("upload: another owner, a buyer and ADMIN are forbidden and nothing is stored")
    void upload_nonManagers_forbidden() {
        when(properties.findByIdForUpdate(PROPERTY_ID)).thenReturn(Optional.of(property(PropertyStatus.PUBLISHED)));

        assertThatThrownBy(() -> service.upload(PROPERTY_ID, List.of(), user(OTHER_ID, "OWNER")))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.upload(PROPERTY_ID, List.of(), user(BUYER_ID, "BUYER_TENANT")))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.upload(PROPERTY_ID, List.of(), user(ADMIN_ID, "ADMIN")))
                .isInstanceOf(ForbiddenOperationException.class);
        verifyNoInteractions(storage);
        verifyNoInteractions(images);
    }

    @Test
    @DisplayName("reorder: another agent and ADMIN are forbidden")
    void reorder_nonManagers_forbidden() {
        givenVisibleLookup(PropertyStatus.PUBLISHED);

        assertThatThrownBy(() -> service.reorder(PROPERTY_ID, List.of(), user(OTHER_ID, "AGENT")))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.reorder(PROPERTY_ID, List.of(), user(ADMIN_ID, "ADMIN")))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    @DisplayName("delete: another owner and a buyer are forbidden")
    void delete_nonManagers_forbidden() {
        givenVisibleLookup(PropertyStatus.PUBLISHED);

        assertThatThrownBy(() -> service.delete(PROPERTY_ID, 1L, user(OTHER_ID, "OWNER")))
                .isInstanceOf(ForbiddenOperationException.class);
        assertThatThrownBy(() -> service.delete(PROPERTY_ID, 1L, user(BUYER_ID, "BUYER_TENANT")))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
