package com.sakan.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jayway.jsonpath.JsonPath;
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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** SCRUM-61 security tests: token attacks, injection input, error leakage and log hygiene on admin sign-in. */
@RegressionTest
@AutoConfigureMockMvc
@Transactional
class AdminSignInSecurityIT extends AbstractIntegrationTest {

    private static final String ADMIN_LOGIN = "/api/auth/admin/login";
    private static final String LOGIN = "/api/auth/login";
    private static final String ME = "/api/auth/me";
    private static final String BUYER_PASSWORD = "Passw0rd1";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProperties jwtProperties;
    @Autowired private LoginAttemptLimiter limiter;

    @Value("${app.admin.username}") private String adminUsername;
    @Value("${app.admin.password}") private String adminPassword;

    // The limiter is a singleton shared by every test class in this Spring context.
    @BeforeEach
    @AfterEach
    void resetLimiter() {
        limiter.reset();
    }

    // ---------- helpers ----------

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private ResultActions post(String path, String username, String password) throws Exception {
        String body = "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(json(username), json(password));
        return mockMvc.perform(MockMvcRequestBuilders.post(path).contentType(MediaType.APPLICATION_JSON).content(body));    }

    private String tokenFrom(ResultActions result) throws Exception {
        return JsonPath.read(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),
                "$.token");
    }

    private String adminToken() throws Exception {
        return tokenFrom(post(ADMIN_LOGIN, adminUsername, adminPassword));
    }

    private User createBuyer(String username) {
        Role role = roleRepository.findByName(RoleName.BUYER_TENANT).orElseThrow();
        return userRepository.save(User.builder()
                .username(username).email(username + "@sakan.com")
                .passwordHash(passwordEncoder.encode(BUYER_PASSWORD)).phone("+201012345678")
                .authProvider(AuthProvider.LOCAL).enabled(true)
                .roles(new HashSet<>(Set.of(role))).build());
    }

    private ResultActions me(String token) throws Exception {
        return mockMvc.perform(get(ME).header("Authorization", "Bearer " + token));
    }

    private String signedToken(String secret, String roleClaim, Instant issuedAt, Instant expiresAt) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secret.getBytes(StandardCharsets.UTF_8)));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("1").issuedAt(issuedAt).expiresAt(expiresAt)
                .claim("username", "whoever").claim("roles", List.of(roleClaim)).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static String b64(String text) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private int seededAdminCount() {
        return jdbc.queryForObject("select count(*) from users where is_seeded_admin = true", Integer.class);
    }

    private void assertNoInternalsLeaked(String body) {
        String lower = body.toLowerCase();
        assertThat(lower).doesNotContain("exception").doesNotContain("org.springframework")
                .doesNotContain("org.postgresql").doesNotContain("com.sakan").doesNotContain("select ")
                .doesNotContain("stacktrace").doesNotContain("postgres");
    }

    // ---------- token attacks (the admin token must not be forgeable) ----------

    @Test
    @DisplayName("S01 admin token with a tampered signature is rejected with 401")
    void tamperedAdminTokenRejected() throws Exception {
        String[] parts = adminToken().split("\\.");
        parts[2] = (parts[2].charAt(0) == 'A' ? 'B' : 'A') + parts[2].substring(1);

        me(String.join(".", parts)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("S02 expired admin token signed with the real secret is rejected with 401")
    void expiredAdminTokenRejected() throws Exception {
        String expired = signedToken(jwtProperties.secret(), "ADMIN",
                Instant.now().minus(Duration.ofHours(2)), Instant.now().minus(Duration.ofHours(1)));

        me(expired).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("S03 token claiming ADMIN but signed with another secret is rejected with 401")
    void forgedAdminTokenRejected() throws Exception {
        String forged = signedToken("forged-secret-forged-secret-0123456789", "ADMIN",
                Instant.now(), Instant.now().plus(Duration.ofHours(1)));

        me(forged).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("S04 unsigned token (alg=none) claiming ADMIN is rejected with 401")
    void algNoneTokenRejected() throws Exception {
        String header = b64("{\"alg\":\"none\",\"typ\":\"JWT\"}");
        String payload = b64("{\"sub\":\"1\",\"username\":\"admin\",\"roles\":[\"ADMIN\"],\"exp\":4102444800}");

        me(header + "." + payload + ".").andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("S05 privilege escalation: a buyer token whose roles claim was edited to ADMIN is rejected")
    void editedRolesClaimRejected() throws Exception {
        User buyer = createBuyer("buyer1");
        String[] parts = tokenFrom(post(LOGIN, "buyer1", BUYER_PASSWORD)).split("\\.");
        parts[1] = b64("{\"sub\":\"%d\",\"username\":\"buyer1\",\"roles\":[\"ADMIN\"],\"exp\":4102444800}"
                .formatted(buyer.getId()));

        me(String.join(".", parts)).andExpect(status().isUnauthorized());
    }

    // ---------- injection and malformed input ----------

    @ParameterizedTest(name = "S06 SQL-injection username [{0}] never signs in and damages nothing")
    @ValueSource(strings = {"admin' OR '1'='1", "admin'--", "'; DROP TABLE users;--", "' OR 1=1 --"})
    void sqlInjectionInUsernameRejected(String payload) throws Exception {
        String body = post(ADMIN_LOGIN, payload, "anything1A")
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertNoInternalsLeaked(body);
        assertThat(seededAdminCount()).isEqualTo(1);   // the users table still exists and is intact
    }

    @Test
    @DisplayName("S07 SQL-injection password for the real admin username never signs in")
    void sqlInjectionInPasswordRejected() throws Exception {
        post(ADMIN_LOGIN, adminUsername, "' OR '1'='1")
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("S08 failed sign-ins leak no stack trace, SQL or class names")
    void errorBodiesLeakNothing() throws Exception {
        createBuyer("buyer2");
        String[] bodies = {
                post(ADMIN_LOGIN, adminUsername, "WrongPass1").andReturn().getResponse().getContentAsString(),
                post(ADMIN_LOGIN, "ghost", "WrongPass1").andReturn().getResponse().getContentAsString(),
                post(ADMIN_LOGIN, "buyer2", BUYER_PASSWORD).andReturn().getResponse().getContentAsString()};

        for (String body : bodies) {
            assertThat(body).isNotBlank();
            assertNoInternalsLeaked(body);
        }
    }

    @Test
    @DisplayName("S09 malformed JSON gives a client error, never a 500 or a token")
    void malformedJsonRejected() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.post(ADMIN_LOGIN).contentType(MediaType.APPLICATION_JSON).content("{\"username\":"))                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("S10 GET on the admin sign-in path is not a way in")
    void getOnAdminLoginRejected() throws Exception {
        mockMvc.perform(get(ADMIN_LOGIN)).andExpect(status().is4xxClientError());
    }

    // ---------- token content and log hygiene ----------

    @Test
    @DisplayName("S11 the admin JWT carries only the minimal claims, no password, hash or email")
    void adminTokenHasMinimalClaims() throws Exception {
        String token = adminToken();
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);

        assertThat(payload).doesNotContain(adminPassword).doesNotContain("$2a$").doesNotContain("$2b$")
                .doesNotContain("passwordHash").doesNotContain("password").doesNotContain("@");
        assertThat(payload).contains("\"roles\"").contains("ADMIN");
    }

    @Test
    @DisplayName("S12 nothing logged during admin sign-in contains the password, a hash or the token")
    void signInLogsNoSecrets() throws Exception {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
        try {
            String token = adminToken();
            post(ADMIN_LOGIN, adminUsername, "WrongPass1").andExpect(status().isUnauthorized());
            post(ADMIN_LOGIN, adminUsername, adminPassword).andExpect(status().isOk());

            List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
            assertThat(messages).noneMatch(m -> m.contains(adminPassword) || m.contains("WrongPass1")
                    || m.contains("$2a$") || m.contains("$2b$") || m.contains(token));
        } finally {
            root.detachAppender(appender);
        }
    }
}