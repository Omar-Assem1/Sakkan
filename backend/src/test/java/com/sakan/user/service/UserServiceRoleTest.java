package com.sakan.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.sakan.support.AbstractIntegrationTest;
import com.sakan.support.tags.IntegrationTest;
import com.sakan.user.domain.AuthProvider;
import com.sakan.user.domain.Role;
import com.sakan.user.domain.RoleName;
import com.sakan.user.domain.User;
import com.sakan.user.repository.RoleRepository;
import com.sakan.user.repository.UserRepository;

/** SCRUM-69: role grant, revoke, count and search against the real database. */
@IntegrationTest
@Transactional
class UserServiceRoleTest extends AbstractIntegrationTest {

    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private JdbcTemplate jdbc;

    // ---------- helpers ----------

    private User createUser(String username, boolean enabled, RoleName... names) {
        Set<Role> roles = new HashSet<>();
        for (RoleName name : names) {
            roles.add(roleRepository.findByName(name).orElseThrow());
        }
        return userRepository.saveAndFlush(User.builder()
                .username(username).email(username + "@sakan.com").passwordHash("x")
                .authProvider(AuthProvider.LOCAL).enabled(enabled).roles(roles).build());
    }

    private int roleRows(long userId, RoleName role) {
        Integer count = jdbc.queryForObject(
                "select count(*) from user_roles ur join roles r on r.id = ur.role_id "
                        + "where ur.user_id = ? and r.name = ?", Integer.class, userId, role.name());
        return count == null ? 0 : count;
    }

    private Set<RoleName> roleNames(User user) {
        return user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
    }

    // ---------- grant ----------

    @Test
    @DisplayName("U01 grantRole inserts the row with granted_by and granted_at and keeps existing roles")
    void grantRole_recordsAudit() {
        User admin = createUser("ur_admin", true, RoleName.ADMIN);
        User target = createUser("ur_target", true, RoleName.BUYER_TENANT);

        User result = userService.grantRole(target.getId(), RoleName.OWNER, admin.getId());

        assertThat(roleNames(result)).containsExactlyInAnyOrder(RoleName.BUYER_TENANT, RoleName.OWNER);
        Map<String, Object> row = jdbc.queryForMap(
                "select ur.granted_by, ur.granted_at from user_roles ur join roles r on r.id = ur.role_id "
                        + "where ur.user_id = ? and r.name = 'OWNER'", target.getId());
        assertThat(((Number) row.get("granted_by")).longValue()).isEqualTo(admin.getId());
        assertThat(row.get("granted_at")).isNotNull();
    }

    @Test
    @DisplayName("U02 grantRole twice leaves exactly one row and throws nothing")
    void grantRole_idempotent() {
        User admin = createUser("ur_admin2", true, RoleName.ADMIN);
        User target = createUser("ur_target2", true, RoleName.BUYER_TENANT);

        userService.grantRole(target.getId(), RoleName.OWNER, admin.getId());
        userService.grantRole(target.getId(), RoleName.OWNER, admin.getId());

        assertThat(roleRows(target.getId(), RoleName.OWNER)).isEqualTo(1);
    }

    // ---------- revoke ----------

    @Test
    @DisplayName("U03 revokeRole removes only that role")
    void revokeRole_removesOnlyThatRole() {
        User target = createUser("ur_target3", true, RoleName.BUYER_TENANT, RoleName.OWNER);

        User result = userService.revokeRole(target.getId(), RoleName.OWNER);

        assertThat(roleNames(result)).containsExactly(RoleName.BUYER_TENANT);
        assertThat(roleRows(target.getId(), RoleName.OWNER)).isZero();
        assertThat(roleRows(target.getId(), RoleName.BUYER_TENANT)).isEqualTo(1);
    }

    // ---------- count ----------

    @Test
    @DisplayName("U04 countByRole counts holders of the role")
    void countByRole_counts() {
        long before = userService.countByRole(RoleName.AGENT);
        createUser("ur_agent1", true, RoleName.AGENT);
        createUser("ur_agent2", true, RoleName.AGENT, RoleName.OWNER);

        assertThat(userService.countByRole(RoleName.AGENT)).isEqualTo(before + 2);
    }

    // ---------- search ----------

    @Test
    @DisplayName("U05 search q matches username or email, case-insensitive")
    void search_textFilter() {
        createUser("zzq_alice", true, RoleName.OWNER);
        createUser("zzq_bob", true, RoleName.OWNER);

        Page<User> byName = userService.search("ZZQ_ALI", null, null, PageRequest.of(0, 10));
        Page<User> byEmail = userService.search("zzq_bob@SAKAN", null, null, PageRequest.of(0, 10));

        assertThat(byName.getContent()).extracting(User::getUsername).containsExactly("zzq_alice");
        assertThat(byEmail.getContent()).extracting(User::getUsername).containsExactly("zzq_bob");
    }

    @Test
    @DisplayName("U06 search filters by role and by enabled")
    void search_roleAndEnabledFilters() {
        createUser("zzr_owner", true, RoleName.OWNER);
        createUser("zzr_agent", false, RoleName.AGENT);

        Page<User> owners = userService.search("zzr", RoleName.OWNER, null, PageRequest.of(0, 10));
        Page<User> disabled = userService.search("zzr", null, false, PageRequest.of(0, 10));

        assertThat(owners.getContent()).extracting(User::getUsername).containsExactly("zzr_owner");
        assertThat(disabled.getContent()).extracting(User::getUsername).containsExactly("zzr_agent");
    }

    @Test
    @DisplayName("U07 search paginates and sorts with a correct total")
    void search_pagination() {
        for (int i = 1; i <= 5; i++) {
            createUser("zzp" + i, true, RoleName.BUYER_TENANT);
        }

        Page<User> page = userService.search("zzp", null, null, PageRequest.of(1, 2, Sort.by("username")));

        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getContent()).extracting(User::getUsername).containsExactly("zzp3", "zzp4");
    }

    @Test
    @DisplayName("U08 LIKE wildcards in q are treated as plain text")
    void search_wildcardsAreLiteral() {
        createUser("zza_b", true, RoleName.OWNER);
        createUser("zzaxb", true, RoleName.OWNER);

        Page<User> underscore = userService.search("zza_b", null, null, PageRequest.of(0, 10));
        Page<User> percent = userService.search("%", null, null, PageRequest.of(0, 10));

        assertThat(underscore.getContent()).extracting(User::getUsername).containsExactly("zza_b");
        assertThat(percent.getTotalElements()).isZero();
    }
}
