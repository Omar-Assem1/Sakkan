package com.sakan.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.sakan.common.config.JwtProperties;
import com.sakan.support.AbstractIntegrationTest;
import com.sakan.user.domain.AuthProvider;
import com.sakan.user.domain.Role;
import com.sakan.user.domain.RoleName;
import com.sakan.user.domain.User;
import com.sakan.user.repository.RoleRepository;
import com.sakan.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
class AuthControllerIT extends AbstractIntegrationTest {

    private static final String BASE = "/api/auth"; // change here if the prefix differs
    private static final String PASSWORD = "Passw0rd1";
    private static final String PHONE = "+201012345678";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProperties jwtProperties;

    // ---------- helpers ----------

    private ResultActions register(String username, String email, String password, String phone, String role)
            throws Exception {
        String body = """
                {"username":"%s","email":"%s","password":"%s","phone":"%s","role":"%s"}
                """.formatted(username, email, password, phone, role);
        return mockMvc.perform(post(BASE + "/register").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions login(String username, String password) throws Exception {
        String body = """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);
        return mockMvc.perform(post(BASE + "/login").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions me(String token) throws Exception {
        return mockMvc.perform(get(BASE + "/me").header("Authorization", "Bearer " + token));
    }

    private User createUser(String username, boolean enabled) {
        Role role = roleRepository.findByName(RoleName.BUYER_TENANT).orElseThrow();
        return userRepository.save(User.builder()
                .username(username)
                .email(username + "@sakan.com")
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .phone(PHONE)
                .authProvider(AuthProvider.LOCAL)
                .enabled(enabled)
                .roles(new HashSet<>(Set.of(role)))
                .build());
    }

    private String loginAndGetToken(String username) throws Exception {
        String json = login(username, PASSWORD).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private NimbusJwtDecoder decoder() {
        return NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(jwtProperties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private String signedToken(String secret, long userId, Instant issuedAt, Instant expiresAt) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(secret.getBytes(StandardCharsets.UTF_8)));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(userId)).issuedAt(issuedAt).expiresAt(expiresAt)
                .claim("username", "omar").claim("roles", List.of("BUYER_TENANT"))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    // ---------- Registration ----------

    // T01
    @Test
    void register_valid_returns201AndSummaryWithoutSecrets() throws Exception {
        register("omar", "omar@sakan.com", PASSWORD, PHONE, "BUYER_TENANT")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value("omar"))
                .andExpect(jsonPath("$.email").value("omar@sakan.com"))
                .andExpect(jsonPath("$.roles[0]").value("BUYER_TENANT"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    // T01 (security): neither the plain password nor a BCrypt hash appears anywhere in the body
    @Test
    void register_responseBodyContainsNoPasswordOrHash() throws Exception {
        String body = register("omar", "omar@sakan.com", PASSWORD, PHONE, "OWNER")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(PASSWORD).doesNotContain("$2a$").doesNotContain("$2b$");
    }

    // T02
    @Test
    void register_duplicateUsername_returns409() throws Exception {
        register("omar", "omar@sakan.com", PASSWORD, PHONE, "OWNER").andExpect(status().isCreated());

        register("omar", "other@sakan.com", PASSWORD, PHONE, "OWNER")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    // T03
    @Test
    void register_duplicateEmail_returns409() throws Exception {
        register("omar", "omar@sakan.com", PASSWORD, PHONE, "OWNER").andExpect(status().isCreated());

        register("omar2", "omar@sakan.com", PASSWORD, PHONE, "OWNER")
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409));
    }

    // T04
    @ParameterizedTest
    @EnumSource(value = RoleName.class, names = {"BUYER_TENANT", "OWNER", "AGENT"})
    void register_storesRequestedRole(RoleName role) throws Exception {
        String username = "user_" + role.name().toLowerCase();
        register(username, username + "@sakan.com", PASSWORD, PHONE, role.name())
                .andExpect(status().isCreated());

        User saved = userRepository.findByUsername(username).orElseThrow();
        assertThat(saved.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .containsExactly(role);
    }

    // T05, T06
    @ParameterizedTest
    @EnumSource(value = RoleName.class, names = {"ADMIN", "MAINTENANCE_STAFF"})
    void register_privilegedRole_returns400AndCreatesNothing(RoleName role) throws Exception {
        register("sneaky", "sneaky@sakan.com", PASSWORD, PHONE, role.name())
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findByUsername("sneaky")).isEmpty();
    }

    // T07
    @ParameterizedTest
    @CsvSource({
            "not-an-email,   Passw0rd1, +201012345678",   // invalid email
            "user@sakan.com, Ab1,       +201012345678",   // too short
            "user@sakan.com, Password,  +201012345678",   // no digit
            "user@sakan.com, 12345678,  +201012345678",   // no letter
            "user@sakan.com, Passw0rd1, 01012345678",     // not E.164 (no plus)
            "user@sakan.com, Passw0rd1, +0123456789",     // E.164 cannot start with 0
            "user@sakan.com, Passw0rd1, +2010abc"         // letters in phone
    })
    void register_invalidInput_returns400(String email, String password, String phone) throws Exception {
        register("validuser", email, password, phone, "BUYER_TENANT")
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findByUsername("validuser")).isEmpty();
    }

    // T07
    @Test
    void register_emptyBody_returns400() throws Exception {
        mockMvc.perform(post(BASE + "/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    // T07
    @Test
    void register_unknownRoleValue_returns400() throws Exception {
        register("omar", "omar@sakan.com", PASSWORD, PHONE, "SUPERUSER")
                .andExpect(status().isBadRequest());
    }

    // T08
    @Test
    void register_persistsBcryptHash() throws Exception {
        register("omar", "omar@sakan.com", PASSWORD, PHONE, "OWNER").andExpect(status().isCreated());

        String stored = userRepository.findByUsername("omar").orElseThrow().getPasswordHash();
        assertThat(stored).startsWith("$2").isNotEqualTo(PASSWORD);
        assertThat(passwordEncoder.matches(PASSWORD, stored)).isTrue();
    }

    // ---------- Login ----------

    // T09
    @Test
    void login_valid_returns200WithJwt() throws Exception {
        createUser("omar", true);

        login("omar", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    // T10
    @Test
    void login_wrongPassword_returns401() throws Exception {
        createUser("omar", true);

        login("omar", "WrongPass1")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    // T11
    @Test
    void login_unknownUser_returns401WithSameMessageAsWrongPassword() throws Exception {
        createUser("omar", true);

        String wrongPassword = login("omar", "WrongPass1")
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String unknownUser = login("ghost", PASSWORD)
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();

        String detailA = JsonPath.read(wrongPassword, "$.detail");
        String detailB = JsonPath.read(unknownUser, "$.detail");
        assertThat(detailB).isEqualTo(detailA);
    }

    // T12
    @Test
    void login_disabledAccount_returns401() throws Exception {
        createUser("omar", false);

        login("omar", PASSWORD).andExpect(status().isUnauthorized());
    }

    // ---------- JWT ----------

    // T13
    @Test
    void issuedToken_isHs256AndExpiresAfter24Hours() throws Exception {
        createUser("omar", true);

        Jwt jwt = decoder().decode(loginAndGetToken("omar"));

        assertThat(jwt.getHeaders().get("alg")).isEqualTo("HS256");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(24));
    }

    // T14
    @Test
    void expiredToken_returns401() throws Exception {
        User user = createUser("omar", true);
        String expired = signedToken(jwtProperties.secret(), user.getId(),
                Instant.now().minus(Duration.ofHours(2)), Instant.now().minus(Duration.ofHours(1)));

        me(expired).andExpect(status().isUnauthorized());
    }

    // T15
    @Test
    void tamperedSignature_returns401() throws Exception {
        createUser("omar", true);
        String[] parts = loginAndGetToken("omar").split("\\.");
        parts[2] = (parts[2].charAt(0) == 'A' ? 'B' : 'A') + parts[2].substring(1);

        me(String.join(".", parts)).andExpect(status().isUnauthorized());
    }

    // T15
    @Test
    void tokenSignedWithOtherSecret_returns401() throws Exception {
        User user = createUser("omar", true);
        String forged = signedToken("forged-secret-forged-secret-0123456789", user.getId(),
                Instant.now(), Instant.now().plus(Duration.ofHours(1)));

        me(forged).andExpect(status().isUnauthorized());
    }

    // T15
    @Test
    void garbageToken_returns401() throws Exception {
        me("not.a.jwt").andExpect(status().isUnauthorized());
    }

    // T15
    @Test
    void noToken_returns401() throws Exception {
        mockMvc.perform(get(BASE + "/me")).andExpect(status().isUnauthorized());
    }

    // T16
    @Test
    void issuedToken_containsUserIdUsernameAndRoles_andIsAccepted() throws Exception {
        User user = createUser("omar", true);
        String token = loginAndGetToken("omar");

        Jwt jwt = decoder().decode(token);
        assertThat(jwt.getSubject()).isEqualTo(String.valueOf(user.getId()));
        assertThat(jwt.getClaimAsString("username")).isEqualTo("omar");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("BUYER_TENANT");

        me(token).andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("omar"))
                .andExpect(content().string(not(containsString("passwordHash"))));
    }
}