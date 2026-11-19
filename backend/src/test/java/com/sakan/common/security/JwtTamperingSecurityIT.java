package com.sakan.common.security;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

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

/** SCRUM-65: expired and tampered tokens, and users holding several roles, against real endpoints. */
@RegressionTest
@AutoConfigureMockMvc
@Transactional
class JwtTamperingSecurityIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Passw0rd1";
    private static final String PHONE = "+201012345678";

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProperties jwtProperties;

    // ---------- helpers ----------

    private User createUser(String username, RoleName... roleNames) {
        Set<Role> roles = Arrays.stream(roleNames)
                .map(n -> roleRepository.findByName(n).orElseThrow())
                .collect(Collectors.toCollection(HashSet::new));
        return userRepository.save(User.builder()
                .username(username).email(username + "@sakan.com")
                .passwordHash(passwordEncoder.encode(PASSWORD)).phone(PHONE)
                .authProvider(AuthProvider.LOCAL).enabled(true)
                .roles(roles).build());
    }

    private String token(long userId, List<String> roles, Instant issuedAt, Instant expiresAt) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(jwtProperties.secret().getBytes(UTF_8)));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(userId)).issuedAt(issuedAt).expiresAt(expiresAt)
                .claim("username", "test").claim("roles", roles).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private String validToken(User user, String... roles) {
        return token(user.getId(), List.of(roles), Instant.now(), Instant.now().plus(Duration.ofHours(1)));
    }

    private String tamperPayload(String token, String from, String to) {
        String[] parts = token.split("\\.");
        String json = new String(Base64.getUrlDecoder().decode(parts[1]), UTF_8);
        assertThat(json).contains(from);
        String forged = Base64.getUrlEncoder().withoutPadding().encodeToString(json.replace(from, to).getBytes(UTF_8));
        return parts[0] + "." + forged + "." + parts[2];
    }

    private ResultActions getWith(String path, String token) throws Exception {
        return mvc.perform(get(path).header("Authorization", "Bearer " + token));
    }

    private int postStatus(String path, String token) throws Exception {
        return mvc.perform(post(path).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();
    }

    // ---------- expired ----------

    @Test
    @DisplayName("T01 expired token returns 401 problem+json on a protected endpoint")
    void expiredToken_401() throws Exception {
        User owner = createUser("expired_owner", RoleName.OWNER);
        String expired = token(owner.getId(), List.of("OWNER"),
                Instant.now().minus(Duration.ofHours(2)), Instant.now().minus(Duration.ofHours(1)));

        getWith("/api/properties/mine", expired)
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        getWith("/api/auth/me", expired).andExpect(status().isUnauthorized());
    }

    // ---------- tampered ----------

    @Test
    @DisplayName("T02 role claim tampered (BUYER_TENANT -> OWNER) keeps the old signature and returns 401")
    void tamperedRole_401() throws Exception {
        User buyer = createUser("tamper_buyer", RoleName.BUYER_TENANT);
        String forged = tamperPayload(validToken(buyer, "BUYER_TENANT"), "BUYER_TENANT", "OWNER");

        getWith("/api/properties/mine", forged).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("T03 subject tampered to impersonate another user returns 401")
    void tamperedSubject_401() throws Exception {
        User owner = createUser("tamper_owner", RoleName.OWNER);
        String forged = tamperPayload(validToken(owner, "OWNER"),
                "\"sub\":\"" + owner.getId() + "\"", "\"sub\":\"1\"");

        getWith("/api/properties/mine", forged).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("T04 signature stripped from a valid token returns 401")
    void strippedSignature_401() throws Exception {
        User owner = createUser("strip_owner", RoleName.OWNER);
        String[] parts = validToken(owner, "OWNER").split("\\.");

        getWith("/api/properties/mine", parts[0] + "." + parts[1] + ".").andExpect(status().isUnauthorized());
    }

    // ---------- several roles ----------

    @Test
    @DisplayName("T05 user with OWNER and BUYER_TENANT passes both role rules but not ADMIN-only ones")
    void multiRoleUser_passesEachAllowedRole() throws Exception {
        User multi = createUser("multi_role", RoleName.OWNER, RoleName.BUYER_TENANT);
        String t = validToken(multi, "OWNER", "BUYER_TENANT");

        getWith("/api/properties/mine", t).andExpect(status().isOk());                  // OWNER rule
        assertThat(postStatus("/api/viewings", t)).isNotIn(401, 403);                    // BUYER_TENANT rule
        assertThat(postStatus("/api/properties", t)).isNotIn(401, 403);                  // OWNER rule
        getWith("/actuator/env", t).andExpect(status().isForbidden());                   // ADMIN only
    }

    @Test
    @DisplayName("T06 single-role BUYER_TENANT is forbidden on listing management but allowed to request viewings")
    void buyerOnly_roleBoundaries() throws Exception {
        User buyer = createUser("buyer_only", RoleName.BUYER_TENANT);
        String t = validToken(buyer, "BUYER_TENANT");

        getWith("/api/properties/mine", t).andExpect(status().isForbidden());
        assertThat(postStatus("/api/properties", t)).isEqualTo(403);
        assertThat(postStatus("/api/viewings", t)).isNotIn(401, 403);
    }

    @Test
    @DisplayName("T07 any authenticated role can call /api/auth/me")
    void meWorksForEveryRole() throws Exception {
        for (RoleName role : List.of(RoleName.BUYER_TENANT, RoleName.OWNER, RoleName.AGENT, RoleName.ADMIN)) {
            User u = createUser("me_" + role.name().toLowerCase(), role);
            getWith("/api/auth/me", validToken(u, role.name())).andExpect(status().isOk());
        }
    }
}
