package com.sakan.admin.web;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import com.sakan.support.AbstractIntegrationTest;
import com.sakan.support.tags.IntegrationTest;
import com.sakan.user.domain.AuthProvider;
import com.sakan.user.domain.Role;
import com.sakan.user.domain.RoleName;
import com.sakan.user.domain.User;
import com.sakan.user.repository.RoleRepository;
import com.sakan.user.repository.UserRepository;

/**
 * SCRUM-69: admin user endpoints end to end. The caller is a real admin row in the DB,
 * because the endpoints re-check the role in the database (decision 2a).
 */
@IntegrationTest
@AutoConfigureMockMvc
@Transactional
class AdminUserControllerIT extends AbstractIntegrationTest {

    private static final String USERS = "/api/admin/users";

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private JdbcTemplate jdbc;
    @Value("${app.admin.username}") private String seededAdminUsername;

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

    /** A promoted (non-seeded) admin who also holds OWNER. */
    private User actor() {
        return createUser("ctl_actor", true, RoleName.ADMIN, RoleName.OWNER);
    }

    private static RequestPostProcessor asAdmin(User admin) {
        return user(String.valueOf(admin.getId())).roles("ADMIN");
    }

    private static String rolesUrl(long id) {
        return USERS + "/" + id + "/roles";
    }

    private static String body(String role) {
        return "{\"role\":\"" + role + "\"}";
    }

    private int roleRows(long userId, RoleName role) {
        Integer count = jdbc.queryForObject(
                "select count(*) from user_roles ur join roles r on r.id = ur.role_id "
                        + "where ur.user_id = ? and r.name = ?", Integer.class, userId, role.name());
        return count == null ? 0 : count;
    }

    // ---------- list ----------

    @Test
    @DisplayName("C01 list returns the page shape and never exposes the password hash")
    void list_shape() throws Exception {
        User admin = actor();
        createUser("ctl_list_a", true, RoleName.OWNER);

        mvc.perform(get(USERS).param("q", "ctl_list").param("size", "5").with(asAdmin(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("ctl_list_a"))
                .andExpect(jsonPath("$.content[0].roles", hasItem("OWNER")))
                .andExpect(jsonPath("$.content[0].seededAdmin").value(false))
                .andExpect(jsonPath("$.content[*].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.content[*].password_hash").doesNotExist());
    }

    @Test
    @DisplayName("C02 list applies the q, role, enabled, page and size parameters")
    void list_filtersAndPaging() throws Exception {
        User admin = actor();
        createUser("ctl_f_owner", true, RoleName.OWNER);
        createUser("ctl_f_agent", false, RoleName.AGENT);

        mvc.perform(get(USERS).param("q", "ctl_f").param("role", "OWNER").with(asAdmin(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("ctl_f_owner"));

        mvc.perform(get(USERS).param("q", "ctl_f").param("enabled", "false").with(asAdmin(admin)))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].username").value("ctl_f_agent"));

        mvc.perform(get(USERS).param("q", "ctl_f").param("size", "1").param("page", "1").with(asAdmin(admin)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    // ---------- grant ----------

    @Test
    @DisplayName("C03 grant adds the role and keeps the existing ones")
    void grant_addsRole() throws Exception {
        User admin = actor();
        User target = createUser("ctl_g", true, RoleName.OWNER);

        mvc.perform(post(rolesUrl(target.getId())).with(asAdmin(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItems("OWNER", "ADMIN")));
    }

    @Test
    @DisplayName("C04 grant stores the acting admin in granted_by")
    void grant_recordsGrantedBy() throws Exception {
        User admin = actor();
        User target = createUser("ctl_g2", true, RoleName.OWNER);

        mvc.perform(post(rolesUrl(target.getId())).with(asAdmin(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body("ADMIN")))
                .andExpect(status().isOk());

        Number grantedBy = jdbc.queryForObject(
                "select ur.granted_by from user_roles ur join roles r on r.id = ur.role_id "
                        + "where ur.user_id = ? and r.name = 'ADMIN'", Number.class, target.getId());
        assertThat(grantedBy.longValue()).isEqualTo(admin.getId());
    }

    @Test
    @DisplayName("C05 granting the same role twice returns 200 both times and keeps one row")
    void grant_idempotent() throws Exception {
        User admin = actor();
        User target = createUser("ctl_g3", true, RoleName.OWNER);

        for (int i = 0; i < 2; i++) {
            mvc.perform(post(rolesUrl(target.getId())).with(asAdmin(admin))
                            .contentType(MediaType.APPLICATION_JSON).content(body("ADMIN")))
                    .andExpect(status().isOk());
        }
        assertThat(roleRows(target.getId(), RoleName.ADMIN)).isEqualTo(1);
    }

    // ---------- revoke ----------

    @Test
    @DisplayName("C06 revoke removes the role and keeps the others")
    void revoke_removesRole() throws Exception {
        User admin = actor();
        User target = createUser("ctl_r", true, RoleName.OWNER, RoleName.ADMIN);

        mvc.perform(delete(rolesUrl(target.getId()) + "/ADMIN").with(asAdmin(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem("OWNER")))
                .andExpect(jsonPath("$.roles", not(hasItem("ADMIN"))));
    }

    @Test
    @DisplayName("C07 the seeded admin can never be demoted (403 problem+json)")
    void revoke_seededAdmin_403() throws Exception {
        User admin = actor();
        User seeded = userRepository.findByUsername(seededAdminUsername).orElseThrow();

        mvc.perform(delete(rolesUrl(seeded.getId()) + "/ADMIN").with(asAdmin(admin)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(roleRows(seeded.getId(), RoleName.ADMIN)).isEqualTo(1);
    }

    @Test
    @DisplayName("C08 an admin cannot demote themselves (403 problem+json)")
    void revoke_self_403() throws Exception {
        User admin = actor();

        mvc.perform(delete(rolesUrl(admin.getId()) + "/ADMIN").with(asAdmin(admin)))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(roleRows(admin.getId(), RoleName.ADMIN)).isEqualTo(1);
    }

    @Test
    @DisplayName("C09 revoke that would leave no role returns 409 problem+json")
    void revoke_lastRole_409() throws Exception {
        User admin = actor();
        User target = createUser("ctl_r2", true, RoleName.OWNER);

        mvc.perform(delete(rolesUrl(target.getId()) + "/OWNER").with(asAdmin(admin)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        assertThat(roleRows(target.getId(), RoleName.OWNER)).isEqualTo(1);
    }

    // ---------- errors ----------

    @Test
    @DisplayName("C10 unknown user id returns 404 problem+json on grant and revoke")
    void unknownUser_404() throws Exception {
        User admin = actor();

        mvc.perform(post(rolesUrl(999_999_999L)).with(asAdmin(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(delete(rolesUrl(999_999_999L) + "/ADMIN").with(asAdmin(admin)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("C11 an unknown role value returns 400 problem+json in the body and in the path")
    void invalidRole_400() throws Exception {
        User admin = actor();
        User target = createUser("ctl_bad", true, RoleName.OWNER);

        mvc.perform(post(rolesUrl(target.getId())).with(asAdmin(admin))
                        .contentType(MediaType.APPLICATION_JSON).content(body("SUPERUSER")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        mvc.perform(delete(rolesUrl(target.getId()) + "/SUPERUSER").with(asAdmin(admin)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("C12 a missing role in the body returns 400 problem+json")
    void missingRole_400() throws Exception {
        User admin = actor();
        User target = createUser("ctl_bad2", true, RoleName.OWNER);

        mvc.perform(post(rolesUrl(target.getId())).with(asAdmin(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
