package com.sakan.auth.security;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
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

@AutoConfigureMockMvc
@Transactional
@ExtendWith(OutputCaptureExtension.class)
class AuthSecurityIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Passw0rd1";
    private static final String PHONE = "+201012345678";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProperties jwtProperties;
    @Autowired private JdbcTemplate jdbc;

    // ---------- helpers ----------

    private User createUser(String username, boolean enabled) {
        Role role = roleRepository.findByName(RoleName.BUYER_TENANT).orElseThrow();
        return userRepository.save(User.builder()
                .username(username).email(username + "@sakan.com")
                .passwordHash(passwordEncoder.encode(PASSWORD)).phone(PHONE)
                .authProvider(AuthProvider.LOCAL).enabled(enabled)
                .roles(new HashSet<>(Set.of(role))).build());
    }

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)));
    }

    private ResultActions register(String username, String email, String role, String extraJson) throws Exception {
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","email":"%s","password":"%s","phone":"%s","role":"%s"%s}"""
                        .formatted(username, email, PASSWORD, PHONE, role, extraJson)));
    }

    private String loginToken(String username) throws Exception {
        String json = login(username, PASSWORD).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private String signed(String secret, long userId, List<String> roles) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secret.getBytes(UTF_8)));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(String.valueOf(userId)).issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofHours(1)))
                .claim("username", "forged").claim("roles", roles).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private ResultActions getWith(String path, String token) throws Exception {
        return mockMvc.perform(get(path).header("Authorization", "Bearer " + token));
    }

    // ---------- 401 / 403 ----------

    @Test @DisplayName("S01 no token returns 401 as problem+json")
    void noToken_401ProblemJson() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test @DisplayName("S02 valid BUYER_TENANT token on an admin endpoint returns 403 as problem+json")
    void wrongRole_403ProblemJson() throws Exception {
        createUser("buyer", true);
        getWith("/api/_probe/admin", loginToken("buyer"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test @DisplayName("S03 sanity: a correctly signed ADMIN token passes the admin probe")
    void adminToken_reachesProbe() throws Exception {
        getWith("/api/_probe/admin", signed(jwtProperties.secret(), 1L, List.of("ADMIN")))
                .andExpect(status().isOk());
    }

    // ---------- JWT attacks not covered by AuthControllerIT ----------

    @Test @DisplayName("S04 tampered payload (BUYER_TENANT -> ADMIN) with the old signature returns 401")
    void tamperedPayload_401() throws Exception {
        createUser("buyer", true);
        String[] p = loginToken("buyer").split("\\.");
        String json = new String(Base64.getUrlDecoder().decode(p[1]), UTF_8);
        assertThat(json).contains("BUYER_TENANT");
        String forged = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(json.replace("BUYER_TENANT", "ADMIN").getBytes(UTF_8));

        getWith("/api/_probe/admin", p[0] + "." + forged + "." + p[2]).andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("S05 unsigned alg=none token claiming ADMIN returns 401")
    void algNone_401() throws Exception {
        var enc = Base64.getUrlEncoder().withoutPadding();
        String header = enc.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(UTF_8));
        long exp = Instant.now().plusSeconds(3600).getEpochSecond();
        String payload = enc.encodeToString(
                ("{\"sub\":\"1\",\"roles\":[\"ADMIN\"],\"exp\":" + exp + "}").getBytes(UTF_8));

        getWith("/api/_probe/admin", header + "." + payload + ".").andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("S06 valid token of a disabled account is rejected on /me")
    void tokenOfDisabledUser_401() throws Exception {
        User user = createUser("later_disabled", true);
        String token = loginToken("later_disabled");
        user.setEnabled(false);
        userRepository.saveAndFlush(user);

        getWith("/api/auth/me", token).andExpect(status().isUnauthorized());
    }

    @Test @DisplayName("S07 correctly signed token for a user id that does not exist returns 401 on /me")
    void tokenOfUnknownUser_401() throws Exception {
        getWith("/api/auth/me", signed(jwtProperties.secret(), 999_999_999L, List.of("BUYER_TENANT")))
                .andExpect(status().isUnauthorized());
    }

    // ---------- Privilege escalation ----------

    @Test @DisplayName("S08 mass assignment: extra JSON fields cannot grant ADMIN or seeded-admin")
    void massAssignment_ignored() throws Exception {
        String extra = ",\"roles\":[\"ADMIN\"],\"isSeededAdmin\":true,\"is_seeded_admin\":true,\"enabled\":false,\"id\":1";
        register("mass", "mass@sakan.com", "BUYER_TENANT", extra);

        var saved = userRepository.findByUsername("mass");
        if (saved.isPresent()) {   // either rejected or created without the extras: both are safe
            assertThat(saved.get().getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                    .containsExactly(RoleName.BUYER_TENANT);
            Boolean seeded = jdbc.queryForObject(
                    "select is_seeded_admin from users where username = ?", Boolean.class, "mass");
            assertThat(seeded).isFalse();
        }
    }

    // ---------- Injection ----------

    @Test @DisplayName("S09 SQL injection strings in login return the generic 401")
    void sqlInjectionLogin_401() throws Exception {
        createUser("victim", true);
        for (String payload : List.of("' OR '1'='1", "victim'--", "x'; DROP TABLE users;--")) {
            login(payload, payload).andExpect(status().isUnauthorized());
        }
        assertThat(jdbc.queryForObject("select count(*) from users where username = 'victim'", Long.class))
                .isEqualTo(1L);
    }

    @Test @DisplayName("S10 SQL injection strings in register are rejected by validation, nothing stored")
    void sqlInjectionRegister_400() throws Exception {
        register("bob'); DROP TABLE users;--", "bob@sakan.com", "OWNER", "").andExpect(status().isBadRequest());
        register("bob", "x'); DROP TABLE roles;--@sakan.com", "OWNER", "").andExpect(status().isBadRequest());

        assertThat(userRepository.findByUsername("bob")).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from roles", Long.class)).isGreaterThanOrEqualTo(5L);
    }

    // ---------- No secrets in responses or logs ----------

    @Test @DisplayName("S11 failed-login and validation error bodies never echo the password or a hash")
    void errorBodiesHaveNoSecrets() throws Exception {
        createUser("omar", true);
        String bad = login("omar", "WrongPass1").andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String invalid = register("omar2", "omar2@sakan.com", "OWNER", "").andReturn().getResponse().getContentAsString();
        String weak = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"weak1\",\"email\":\"w@sakan.com\",\"password\":\"password\","
                                + "\"phone\":\"" + PHONE + "\",\"role\":\"OWNER\"}"))
                .andExpect(status().isBadRequest()).andReturn().getResponse().getContentAsString();

        assertThat(bad).doesNotContain("WrongPass1", "$2a$", "$2b$");
        assertThat(invalid).doesNotContain("$2a$", "$2b$");
        assertThat(weak).doesNotContain("password\"", "\"password\":\"password");
    }

    @Test @DisplayName("S12 logs never contain passwords, hashes, tokens or the JWT secret")
    void logsHaveNoSecrets(CapturedOutput output) throws Exception {
        register("logcheck", "logcheck@sakan.com", "AGENT", "").andExpect(status().isCreated());
        String token = loginToken("logcheck");
        login("logcheck", "WrongPass1");
        getWith("/api/auth/me", token);
        getWith("/api/auth/me", token + "x");

        String hash = userRepository.findByUsername("logcheck").orElseThrow().getPasswordHash();
        assertThat(output.getAll()).doesNotContain(PASSWORD, "WrongPass1", jwtProperties.secret(), token, hash);
    }
}