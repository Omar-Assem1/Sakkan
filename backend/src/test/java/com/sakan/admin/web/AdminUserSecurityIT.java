package com.sakan.admin.web;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sakan.common.config.JwtProperties;
import com.sakan.support.AbstractIntegrationTest;
import com.sakan.support.tags.RegressionTest;
import com.sakan.user.domain.AuthProvider;
import com.sakan.user.domain.Role;
import com.sakan.user.domain.RoleName;
import com.sakan.user.domain.User;
import com.sakan.user.repository.RoleRepository;
import com.sakan.user.repository.UserRepository;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Security regressions specific to the SCRUM-69 admin user endpoints. */
@RegressionTest
@AutoConfigureMockMvc
@Transactional
class AdminUserSecurityIT extends AbstractIntegrationTest {

    private static final String PASSWORD_HASH = "$2a$10$not-a-real-hash-for-security-fixture";
    private static final String USERS = "/api/admin/users";

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository users;
    @Autowired private RoleRepository roles;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private JwtProperties jwtProperties;

    private User createUser(String username, RoleName... names) {
        Set<Role> assigned = new HashSet<>();
        for (RoleName name : names) assigned.add(roles.findByName(name).orElseThrow());
        return users.saveAndFlush(User.builder().username(username).email(username + "@sakan.test")
                .passwordHash(PASSWORD_HASH).authProvider(AuthProvider.LOCAL).enabled(true).roles(assigned).build());
    }

    private String token(User user, String... roleClaims) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(jwtProperties.secret().getBytes(UTF_8)));
        JwtClaimsSet claims = JwtClaimsSet.builder().subject(user.getId().toString())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600))
                .claim("username", user.getUsername()).claim("roles", List.of(roleClaims)).build();
        return encoder.encode(JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    @Test
    @DisplayName("A valid ADMIN JWT stops authorizing admin endpoints after the DB role is revoked")
    void revokedAdminClaim_isRejectedOnEveryAdminEndpoint() throws Exception {
        User formerAdmin = createUser("s69_revoked_admin", RoleName.ADMIN, RoleName.OWNER);
        User target = createUser("s69_revoked_target", RoleName.OWNER);
        String token = token(formerAdmin, "ADMIN");

        formerAdmin.getRoles().remove(roles.findByName(RoleName.ADMIN).orElseThrow());
        users.saveAndFlush(formerAdmin);

        mvc.perform(get(USERS).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(post(USERS + "/" + target.getId() + "/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"AGENT\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete(USERS + "/" + target.getId() + "/roles/OWNER")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertThat(jdbc.queryForObject("select count(*) from user_roles ur join roles r on r.id = ur.role_id "
                + "where ur.user_id = ? and r.name = 'AGENT'", Integer.class, target.getId())).isZero();
    }

    @Test
    @DisplayName("An injection-style user-list filter is treated as literal text")
    void sqlInjectionFilter_doesNotExposeUsersOrChangeDatabase() throws Exception {
        User admin = createUser("s69_query_admin", RoleName.ADMIN);
        createUser("s69_query_target", RoleName.OWNER);
        long before = jdbc.queryForObject("select count(*) from users", Long.class);

        mvc.perform(get(USERS).param("q", "%' OR '1'='1 --")
                        .header("Authorization", "Bearer " + token(admin, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        assertThat(jdbc.queryForObject("select count(*) from users", Long.class)).isEqualTo(before);
        assertThat(jdbc.queryForObject("select count(*) from roles", Integer.class)).isGreaterThanOrEqualTo(5);
    }
}
