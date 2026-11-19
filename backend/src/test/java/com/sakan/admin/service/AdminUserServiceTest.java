package com.sakan.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.sakan.admin.dto.AdminUserResponse;
import com.sakan.admin.dto.AdminUserSearchCriteria;
import com.sakan.common.exception.ForbiddenOperationException;
import com.sakan.common.exception.ResourceNotFoundException;
import com.sakan.common.exception.RoleConflictException;
import com.sakan.support.tags.UnitTest;
import com.sakan.user.domain.Role;
import com.sakan.user.domain.RoleName;
import com.sakan.user.domain.User;
import com.sakan.user.service.UserService;

/** SCRUM-69: all business rules of admin user management, no Spring and no DB. */
@UnitTest
@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    private static final long ACTOR_ID = 1L;
    private static final long TARGET_ID = 2L;

    @Mock private UserService userService;
    @Mock private AdminUserMapper mapper;
    @InjectMocks private AdminUserServiceImpl service;

    // ---------- helpers ----------

    private static Role role(RoleName name) {
        return Role.builder().name(name).build();
    }

    private static User user(long id, boolean seeded, RoleName... names) {
        Set<Role> roles = new HashSet<>();
        for (RoleName name : names) {
            roles.add(role(name));
        }
        return User.builder().id(id).username("u" + id).email("u" + id + "@sakan.com")
                .seededAdmin(seeded).roles(roles).build();
    }

    private static AdminUserResponse response(User u) {
        return new AdminUserResponse(u.getId(), u.getUsername(), u.getEmail(), null,
                List.of(), u.isEnabled(), u.isSeededAdmin(), "LOCAL");
    }

    // ---------- search ----------

    @Test
    @DisplayName("S01 search passes criteria and pageable to UserService and maps the page")
    void search_mapsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        User u = user(TARGET_ID, false, RoleName.OWNER);
        AdminUserResponse r = response(u);
        when(userService.search("ali", RoleName.OWNER, true, pageable))
                .thenReturn(new PageImpl<>(List.of(u), pageable, 1));
        when(mapper.toResponse(u)).thenReturn(r);

        var result = service.search(new AdminUserSearchCriteria("ali", RoleName.OWNER, true), pageable);

        assertThat(result.content()).containsExactly(r);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    // ---------- grant ----------

    @Test
    @DisplayName("S02 grant adds the role and records the acting admin")
    void grant_addsRole() {
        User before = user(TARGET_ID, false, RoleName.BUYER_TENANT);
        User after = user(TARGET_ID, false, RoleName.BUYER_TENANT, RoleName.ADMIN);
        AdminUserResponse r = response(after);
        when(userService.findById(TARGET_ID)).thenReturn(Optional.of(before));
        when(userService.grantRole(TARGET_ID, RoleName.ADMIN, ACTOR_ID)).thenReturn(after);
        when(mapper.toResponse(after)).thenReturn(r);

        assertThat(service.grantRole(ACTOR_ID, TARGET_ID, RoleName.ADMIN)).isEqualTo(r);
        verify(userService).grantRole(TARGET_ID, RoleName.ADMIN, ACTOR_ID);
    }

    @Test
    @DisplayName("S03 grant of a role already held is a no-op")
    void grant_alreadyHeld_noop() {
        User u = user(TARGET_ID, false, RoleName.OWNER, RoleName.ADMIN);
        AdminUserResponse r = response(u);
        when(userService.findById(TARGET_ID)).thenReturn(Optional.of(u));
        when(mapper.toResponse(u)).thenReturn(r);

        assertThat(service.grantRole(ACTOR_ID, TARGET_ID, RoleName.ADMIN)).isEqualTo(r);
        verify(userService, never()).grantRole(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("S04 grant to an unknown user throws ResourceNotFoundException")
    void grant_unknownUser() {
        when(userService.findById(TARGET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.grantRole(ACTOR_ID, TARGET_ID, RoleName.ADMIN))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(userService, never()).grantRole(anyLong(), any(), anyLong());
    }

    // ---------- revoke ----------

    @Test
    @DisplayName("S05 revoking any role from the seeded admin is forbidden")
    void revoke_seededAdmin_forbidden() {
        when(userService.findById(TARGET_ID)).thenReturn(Optional.of(user(TARGET_ID, true, RoleName.ADMIN)));

        assertThatThrownBy(() -> service.revokeRole(ACTOR_ID, TARGET_ID, RoleName.ADMIN))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(userService, never()).revokeRole(anyLong(), any());
    }

    @Test
    @DisplayName("S06 the seeded-admin rule wins over the last-admin rule")
    void revoke_seededAdmin_priorityOverLastAdmin() {
        when(userService.findById(TARGET_ID)).thenReturn(Optional.of(user(TARGET_ID, true, RoleName.ADMIN)));

        assertThatThrownBy(() -> service.revokeRole(ACTOR_ID, TARGET_ID, RoleName.ADMIN))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(userService, never()).countByRole(any());
    }

    @Test
    @DisplayName("S07 an admin cannot revoke ADMIN from themselves")
    void revoke_selfDemote_forbidden() {
        when(userService.findById(ACTOR_ID))
                .thenReturn(Optional.of(user(ACTOR_ID, false, RoleName.ADMIN, RoleName.OWNER)));

        assertThatThrownBy(() -> service.revokeRole(ACTOR_ID, ACTOR_ID, RoleName.ADMIN))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(userService, never()).revokeRole(anyLong(), any());
    }

    @Test
    @DisplayName("S08 an admin may revoke a non-admin role from themselves")
    void revoke_selfOtherRole_allowed() {
        User before = user(ACTOR_ID, false, RoleName.ADMIN, RoleName.OWNER);
        User after = user(ACTOR_ID, false, RoleName.ADMIN);
        AdminUserResponse r = response(after);
        when(userService.findById(ACTOR_ID)).thenReturn(Optional.of(before));
        when(userService.revokeRole(ACTOR_ID, RoleName.OWNER)).thenReturn(after);
        when(mapper.toResponse(after)).thenReturn(r);

        assertThat(service.revokeRole(ACTOR_ID, ACTOR_ID, RoleName.OWNER)).isEqualTo(r);
    }

    @Test
    @DisplayName("S09 revoking ADMIN when it is the only admin left throws RoleConflictException")
    void revoke_lastAdmin_conflict() {
        when(userService.findById(TARGET_ID))
                .thenReturn(Optional.of(user(TARGET_ID, false, RoleName.ADMIN, RoleName.OWNER)));
        when(userService.countByRole(RoleName.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.revokeRole(ACTOR_ID, TARGET_ID, RoleName.ADMIN))
                .isInstanceOf(RoleConflictException.class);
        verify(userService, never()).revokeRole(anyLong(), any());
    }

    @Test
    @DisplayName("S10 revoke that would leave the user with no role throws RoleConflictException")
    void revoke_lastRole_conflict() {
        when(userService.findById(TARGET_ID)).thenReturn(Optional.of(user(TARGET_ID, false, RoleName.OWNER)));

        assertThatThrownBy(() -> service.revokeRole(ACTOR_ID, TARGET_ID, RoleName.OWNER))
                .isInstanceOf(RoleConflictException.class);
        verify(userService, never()).revokeRole(anyLong(), any());
    }

    @Test
    @DisplayName("S11 revoke of a role the user does not hold is a no-op")
    void revoke_notHeld_noop() {
        User u = user(TARGET_ID, false, RoleName.BUYER_TENANT);
        AdminUserResponse r = response(u);
        when(userService.findById(TARGET_ID)).thenReturn(Optional.of(u));
        when(mapper.toResponse(u)).thenReturn(r);

        assertThat(service.revokeRole(ACTOR_ID, TARGET_ID, RoleName.ADMIN)).isEqualTo(r);
        verify(userService, never()).revokeRole(anyLong(), any());
    }

    @Test
    @DisplayName("S12 revoke of ADMIN from a promoted admin succeeds")
    void revoke_promotedAdmin_success() {
        User before = user(TARGET_ID, false, RoleName.OWNER, RoleName.ADMIN);
        User after = user(TARGET_ID, false, RoleName.OWNER);
        AdminUserResponse r = response(after);
        when(userService.findById(TARGET_ID)).thenReturn(Optional.of(before));
        when(userService.countByRole(RoleName.ADMIN)).thenReturn(2L);
        when(userService.revokeRole(TARGET_ID, RoleName.ADMIN)).thenReturn(after);
        when(mapper.toResponse(after)).thenReturn(r);

        assertThat(service.revokeRole(ACTOR_ID, TARGET_ID, RoleName.ADMIN)).isEqualTo(r);
        verify(userService).revokeRole(TARGET_ID, RoleName.ADMIN);
    }

    @Test
    @DisplayName("S13 revoke on an unknown user throws ResourceNotFoundException")
    void revoke_unknownUser() {
        when(userService.findById(TARGET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revokeRole(ACTOR_ID, TARGET_ID, RoleName.OWNER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- future delete endpoint ----------

    @Test
    @DisplayName("S14 assertDeletable rejects the seeded admin, accepts others, 404 for unknown")
    void assertDeletable_rules() {
        when(userService.findById(1L)).thenReturn(Optional.of(user(1L, true, RoleName.ADMIN)));
        when(userService.findById(2L)).thenReturn(Optional.of(user(2L, false, RoleName.OWNER)));
        when(userService.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assertDeletable(1L)).isInstanceOf(ForbiddenOperationException.class);
        assertThatCode(() -> service.assertDeletable(2L)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.assertDeletable(3L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
