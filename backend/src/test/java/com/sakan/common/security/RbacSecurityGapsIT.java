package com.sakan.common.security;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
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

/**
 * SCRUM-65 step 6: gaps left by JwtTamperingSecurityIT and RbacEndpointMatrixIT.
 * Role claim edge cases, algorithm and key attacks, disabled users, secret leakage,
 * and the documented limits of stateless JWT roles.
 */
@RegressionTest
@AutoConfigureMockMvc
@Transactional
class RbacSecurityGapsIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Passw0rd1";
    private static final String PHONE = "+201012345678";

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProperties jwtProperties;

    // ---------- helpers ----------

    private User createUser(String username, boolean enabled, RoleName... roleNames) {
        Set<Role> roles = Arrays.stream(roleNames)
                .map(n -> roleRepository.findByName(n).orElseThrow())
                .collect(Collectors.toCollection(HashSet::new));
        return userRepository.save(User.builder()
                .username(username).email(username + "@sakan.com")
                .passwordHash(passwordEncoder.encode(PASSWORD)).phone(PHONE)
                .authProvider(AuthProvider.LOCAL).enabled(enabled)
                .roles(roles).build());
    }

    private JwtClaimsSet.Builder baseClaims(long userId) {
        return JwtClaimsSet.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofHours(1)))
                .claim("username", "test");
    }

    private String sign(JwtClaimsSet claims, byte[] secret) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secret));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private byte[] secret() {
        return jwtProperties.secret().getBytes(UTF_8);
    }

    private String tokenWithRoles(long userId, List<String> roles) {
        return sign(baseClaims(userId).claim("roles", roles).build(), secret());
    }

    private int getStatus(String path, String token) throws Exception {
        return mvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getStatus();
    }

    private int postStatus(String path, String token) throws Exception {
        return mvc.perform(post(path).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();
    }

    private MockHttpServletResponse login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andReturn().getResponse();
    }

    // ---------- role claim edge cases ----------

    @Test
    @DisplayName("S01 valid signature but no roles claim: authenticated, yet no role-protected endpoint opens")
    void noRolesClaim_forbiddenEverywhereRoleProtected() throws Exception {
        User user = createUser("no_roles_claim", true, RoleName.OWNER);
        String token = sign(baseClaims(user.getId()).build(), secret());

        assertThat(getStatus("/api/properties/mine", token)).isEqualTo(403);
        assertThat(postStatus("/api/properties", token)).isEqualTo(403);
        assertThat(postStatus("/api/viewings", token)).isEqualTo(403);
        assertThat(getStatus("/actuator/env", token)).isEqualTo(403);
    }

    @Test
    @DisplayName("S02 empty roles list behaves like no roles")
    void emptyRolesList_forbidden() throws Exception {
        User user = createUser("empty_roles", true, RoleName.OWNER);
        String token = tokenWithRoles(user.getId(), List.of());

        assertThat(getStatus("/api/properties/mine", token)).isEqualTo(403);
        assertThat(getStatus("/actuator/env", token)).isEqualTo(403);
    }

    @ParameterizedTest(name = "unknown role claim \"{0}\" never grants ADMIN")
    @ValueSource(strings = {"SUPERUSER", "ROLE_ADMIN", "admin", "ADMIN ", "*"})
    @DisplayName("S03 role names that only look like ADMIN do not open admin-only endpoints")
    void lookalikeRole_doesNotGrantAdmin(String claim) throws Exception {
        User user = createUser("lookalike_" + Math.abs(claim.hashCode()), true, RoleName.BUYER_TENANT);
        String token = tokenWithRoles(user.getId(), List.of(claim));

        assertThat(getStatus("/actuator/env", token)).as("claim %s", claim).isEqualTo(403);
    }

    // ---------- algorithm and key attacks ----------

    @Test
    @DisplayName("S04 alg=none token claiming ADMIN is rejected with 401")
    void algNone_401() throws Exception {
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String header = b64.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(UTF_8));
        long exp = Instant.now().plus(Duration.ofHours(1)).getEpochSecond();
        String payload = b64.encodeToString(
                ("{\"sub\":\"1\",\"roles\":[\"ADMIN\"],\"exp\":" + exp + "}").getBytes(UTF_8));
        String forged = header + "." + payload + ".";

        assertThat(getStatus("/actuator/env", forged)).isEqualTo(401);
        assertThat(getStatus("/api/auth/me", forged)).isEqualTo(401);
    }

    @Test
    @DisplayName("S05 token signed with a different secret is rejected with 401, even when it claims ADMIN")
    void wrongSecret_401() throws Exception {
        User user = createUser("wrong_secret", true, RoleName.BUYER_TENANT);
        byte[] otherSecret = "another-secret-that-is-at-least-32-bytes-long!!".getBytes(UTF_8);
        String forged = sign(baseClaims(user.getId()).claim("roles", List.of("ADMIN")).build(), otherSecret);

        assertThat(getStatus("/actuator/env", forged)).isEqualTo(401);
        assertThat(getStatus("/api/properties/mine", forged)).isEqualTo(401);
    }

    @Test
    @DisplayName("S06 non-numeric subject never produces a 5xx; it is denied")
    void nonNumericSubject_denied() throws Exception {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("not-a-number").issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofHours(1)))
                .claim("roles", List.of("OWNER")).build();
        String token = sign(claims, secret());

        assertThat(getStatus("/api/properties/mine", token)).isIn(401, 403);
    }

    // ---------- disabled users, secrets, hostile input ----------

    @Test
    @DisplayName("S07 a disabled user cannot sign in, and no token is returned")
    void disabledUser_cannotLogin() throws Exception {
        createUser("disabled_user", false, RoleName.OWNER);

        MockHttpServletResponse response = login("disabled_user", PASSWORD);

        assertThat(response.getStatus()).isIn(401, 403);
        assertThat(response.getContentAsString()).doesNotContain("token");
    }

    @Test
    @DisplayName("S08 /api/auth/me never exposes password data")
    void me_neverExposesPassword() throws Exception {
        User user = createUser("me_profile_check", true, RoleName.BUYER_TENANT);
        String body = mvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + tokenWithRoles(user.getId(), List.of("BUYER_TENANT"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body.toLowerCase()).doesNotContain("password").doesNotContain("hash").doesNotContain("$2a$");
    }

    @Test
    @DisplayName("S09 a failed login does not echo the password or hash, and an injection-style username is just a failed login")
    void failedLogin_leaksNothing() throws Exception {
        createUser("leak_check", true, RoleName.BUYER_TENANT);

        String wrong = login("leak_check", "WrongPass1").getContentAsString();
        assertThat(wrong).doesNotContain("WrongPass1").doesNotContain("$2a$").doesNotContain("token");

        MockHttpServletResponse injection = login("' OR '1'='1' --", "x");
        assertThat(injection.getStatus()).isIn(400, 401);
        assertThat(injection.getContentAsString()).doesNotContain("token");
    }

    // ---------- documented limits of stateless JWT roles (behavior NOT changed) ----------

    @Test
    @DisplayName("S10 KNOWN LIMITATION: a role removed in the DB stays valid until the token expires")
    void staleRoleInToken_isStillHonoured() throws Exception {
        // DB says BUYER_TENANT only, but the (still valid) token was issued when the user was an OWNER.
        User user = createUser("stale_role", true, RoleName.BUYER_TENANT);
        String staleToken = tokenWithRoles(user.getId(), List.of("OWNER"));

        assertThat(getStatus("/api/properties/mine", staleToken)).isEqualTo(200);
    }

    @Test
    @DisplayName("S11 KNOWN LIMITATION: a token issued before the user was disabled is still accepted")
    void tokenIssuedBeforeDisable_isStillAccepted() throws Exception {
        User user = createUser("disabled_later", true, RoleName.OWNER);
        String token = tokenWithRoles(user.getId(), List.of("OWNER"));
        user.setEnabled(false);
        userRepository.saveAndFlush(user);

        assertThat(getStatus("/api/properties/mine", token)).isEqualTo(200);
    }
}