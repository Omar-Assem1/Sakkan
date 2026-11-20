package com.sakan.auth.security;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sakan.common.config.JwtProperties;
import com.sakan.support.AbstractIntegrationTest;
import com.sakan.support.tags.RegressionTest;
import com.sakan.user.domain.AuthProvider;
import com.sakan.user.domain.Role;
import com.sakan.user.domain.RoleName;
import com.sakan.user.domain.User;
import com.sakan.user.repository.RoleRepository;
import com.sakan.user.repository.UserRepository;
import java.security.MessageDigest;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** SCRUM-56: Google sign-in, account resolution, profile completion, and one-time JWT handoff. */
@RegressionTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-client-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-client-secret",
        "spring.security.oauth2.client.registration.google.scope=openid,profile,email",
        "spring.security.oauth2.client.provider.google.issuer-uri=https://accounts.google.com"})
class GoogleOAuthFlowIT extends AbstractIntegrationTest {

    private static final String EXCHANGE = "/api/auth/google/exchange";
    private static final String COMPLETE = "/api/auth/google/complete";
    private static final String PASSWORD = "LocalPassw0rd1";

    @Autowired private MockMvc mvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtProperties jwtProperties;

    private User createUser(String username, String email, AuthProvider provider, String googleSub,
            RoleName... roleNames) {
        Set<Role> assignedRoles = new HashSet<>();
        for (RoleName roleName : roleNames) {
            assignedRoles.add(roleRepository.findByName(roleName).orElseThrow());
        }
        return userRepository.saveAndFlush(User.builder().username(username).email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD)).phone("+201012345678")
                .authProvider(provider).googleSub(googleSub).enabled(true).roles(assignedRoles).build());
    }

    /** Seed an identity already verified by the Google callback; the raw code is never stored. */
    private void seedHandoff(String code, String sub, String email, boolean emailVerified,
            String hostedDomain, Instant expiresAt) throws Exception {
        jdbc.update("insert into google_login_handoffs "
                        + "(code_hash, google_sub, email, email_verified, hosted_domain, expires_at) "
                        + "values (?, ?, ?, ?, ?, ?)",
                sha256(code), sub, email, emailVerified, hostedDomain, Timestamp.from(expiresAt));
    }

    private static String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(UTF_8));
        return HexFormat.of().formatHex(digest);
    }

    private ResultActions exchange(String code) throws Exception {
        return mvc.perform(post(EXCHANGE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\"}"));
    }

    private ResultActions complete(String code, String username) throws Exception {
        return mvc.perform(post(COMPLETE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"username\":\"" + username + "\"}"));
    }

    @Test
    @DisplayName("G01 Continue with Google starts an authorization-code redirect with state")
    void authorizationRedirect_hasClientScopeAndState() throws Exception {
        mvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", allOf(
                        containsString("accounts.google.com"),
                        containsString("client_id=test-google-client-id"),
                        containsString("redirect_uri=http://localhost/login/oauth2/code/google"),
                        containsString("scope="), containsString("openid"), containsString("state="))));
    }

    @Test
    @DisplayName("G02 first Google sign-in asks for username, then creates a Buyer/Tenant account and Sakan JWT")
    void firstLogin_completesUsernameAndCreatesAccount() throws Exception {
        String code = "first-login-code";
        seedHandoff(code, "google-sub-first", "first@gmail.com", true, null,
                Instant.now().plusSeconds(300));

        exchange(code).andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationRequired").value(true))
                .andExpect(jsonPath("$.token").doesNotExist());

        String response = complete(code, "first_google_user").andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        User created = userRepository.findAll().stream()
                .filter(user -> "google-sub-first".equals(user.getGoogleSub())).findFirst().orElseThrow();
        assertThat(created.getUsername()).isEqualTo("first_google_user");
        assertThat(created.getEmail()).isEqualTo("first@gmail.com");
        assertThat(created.getAuthProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(created.getPhone()).isNull();
        assertThat(created.getRoles()).extracting(Role::getName).containsExactly(RoleName.BUYER_TENANT);

        String token = com.jayway.jsonpath.JsonPath.read(response, "$.token");
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(created.getId()));
    }

    @Test
    @DisplayName("G03 repeated Google login finds the account by sub and does not duplicate it")
    void repeatLogin_matchesGoogleSubject() throws Exception {
        User existing = createUser("repeat_google", "old-address@example.com", AuthProvider.GOOGLE,
                "stable-google-sub", RoleName.BUYER_TENANT);
        String code = "repeat-login-code";
        seedHandoff(code, "stable-google-sub", "new-address@example.com", true, null,
                Instant.now().plusSeconds(300));

        exchange(code).andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationRequired").value(false))
                .andExpect(jsonPath("$.token").isNotEmpty());

        assertThat(userRepository.findAll().stream()
                .filter(user -> "stable-google-sub".equals(user.getGoogleSub())).count()).isEqualTo(1);
        assertThat(userRepository.findById(existing.getId())).isPresent();
    }

    @Test
    @DisplayName("G04 authoritative verified Gmail may link a local account and preserves its password login")
    void verifiedGmail_linksLocalAccountWithoutReplacingPassword() throws Exception {
        User local = createUser("local_gmail", "person@gmail.com", AuthProvider.LOCAL, null, RoleName.OWNER);
        String savedHash = local.getPasswordHash();
        String code = "gmail-link-code";
        seedHandoff(code, "gmail-google-sub", "person@gmail.com", true, null,
                Instant.now().plusSeconds(300));

        exchange(code).andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationRequired").value(false))
                .andExpect(jsonPath("$.token").isNotEmpty());

        User linked = userRepository.findById(local.getId()).orElseThrow();
        assertThat(linked.getGoogleSub()).isEqualTo("gmail-google-sub");
        assertThat(linked.getAuthProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(linked.getPasswordHash()).isEqualTo(savedHash);
        assertThat(linked.getRoles()).extracting(Role::getName).containsExactly(RoleName.OWNER);
    }

    @Test
    @DisplayName("G05 verified Workspace identity may link only when its hosted domain is present")
    void verifiedWorkspace_linksLocalAccount() throws Exception {
        User local = createUser("local_workspace", "member@company.example", AuthProvider.LOCAL, null,
                RoleName.AGENT);
        String code = "workspace-link-code";
        seedHandoff(code, "workspace-google-sub", "member@company.example", true, "company.example",
                Instant.now().plusSeconds(300));

        exchange(code).andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationRequired").value(false));

        assertThat(userRepository.findById(local.getId()).orElseThrow().getGoogleSub())
                .isEqualTo("workspace-google-sub");
    }

    @Test
    @DisplayName("G06 verified third-party email collision is rejected and does not link")
    void externalEmailCollision_requiresExplicitLink() throws Exception {
        User local = createUser("local_external", "person@outside.example", AuthProvider.LOCAL, null,
                RoleName.OWNER);
        String code = "external-conflict-code";
        seedHandoff(code, "external-google-sub", "person@outside.example", true, null,
                Instant.now().plusSeconds(300));

        exchange(code).andExpect(status().isConflict());

        assertThat(userRepository.findById(local.getId()).orElseThrow().getGoogleSub()).isNull();
    }

    @Test
    @DisplayName("G07 unverified Google email cannot create an account")
    void unverifiedEmail_rejected() throws Exception {
        String code = "unverified-email-code";
        seedHandoff(code, "unverified-google-sub", "not-verified@example.com", false, null,
                Instant.now().plusSeconds(300));

        exchange(code).andExpect(status().isUnauthorized());
        assertThat(userRepository.findAll().stream()
                .noneMatch(user -> "unverified-google-sub".equals(user.getGoogleSub()))).isTrue();
    }

    @Test
    @DisplayName("G08 expired or replayed one-time handoff codes cannot issue a JWT")
    void expiredAndReplayedCode_rejected() throws Exception {
        String expired = "expired-google-code";
        seedHandoff(expired, "expired-google-sub", "expired@gmail.com", true, null,
                Instant.now().minusSeconds(1));
        exchange(expired).andExpect(status().isUnauthorized());

        String code = "replayed-google-code";
        createUser("replayed_google", "replayed@gmail.com", AuthProvider.GOOGLE,
                "replayed-google-sub", RoleName.BUYER_TENANT);
        seedHandoff(code, "replayed-google-sub", "replayed@gmail.com", true, null,
                Instant.now().plusSeconds(300));
        exchange(code).andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty());
        exchange(code).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("G09 username is required, validated, and unique during first Google login")
    void usernameCompletion_validatesAndRejectsDuplicates() throws Exception {
        createUser("taken_google_name", "taken@example.com", AuthProvider.LOCAL, null, RoleName.OWNER);
        String code = "username-validation-code";
        seedHandoff(code, "username-google-sub", "new-google@gmail.com", true, null,
                Instant.now().plusSeconds(300));
        exchange(code).andExpect(status().isOk()).andExpect(jsonPath("$.registrationRequired").value(true));

        complete(code, "bad username!").andExpect(status().isBadRequest());
        complete(code, "taken_google_name").andExpect(status().isConflict());
        complete(code, "available_google_name").andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    @DisplayName("G10 injection-style username input is rejected and does not create an account")
    void injectionUsername_rejected() throws Exception {
        String code = "injection-username-code";
        seedHandoff(code, "injection-google-sub", "injection@gmail.com", true, null,
                Instant.now().plusSeconds(300));
        exchange(code).andExpect(status().isOk());

        complete(code, "' OR '1'='1 --").andExpect(status().isBadRequest());
        assertThat(userRepository.findAll().stream()
                .noneMatch(user -> "injection-google-sub".equals(user.getGoogleSub()))).isTrue();
    }

    @Test
    @DisplayName("G11 missing and unknown handoff codes never issue a JWT")
    void missingOrUnknownCode_rejected() throws Exception {
        mvc.perform(post(EXCHANGE).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        exchange("not-a-real-code").andExpect(status().isUnauthorized());
    }
}
