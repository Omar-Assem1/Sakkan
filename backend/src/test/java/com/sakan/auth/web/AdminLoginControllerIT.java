package com.sakan.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sakan.auth.security.LoginAttemptLimiter;
import com.sakan.support.AbstractIntegrationTest;
import com.sakan.support.tags.RegressionTest;
import com.sakan.user.domain.AuthProvider;
import com.sakan.user.domain.Role;
import com.sakan.user.domain.RoleName;
import com.sakan.user.domain.User;
import com.sakan.user.repository.RoleRepository;
import com.sakan.user.repository.UserRepository;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@RegressionTest
@AutoConfigureMockMvc
@Transactional
class AdminLoginControllerIT extends AbstractIntegrationTest {

    private static final String ADMIN_LOGIN = "/api/auth/admin/login";
    private static final String BUYER_PASSWORD = "Passw0rd1";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private LoginAttemptLimiter limiter;

    @Value("${app.admin.username}") private String adminUsername;
    @Value("${app.admin.password}") private String adminPassword;

    // The limiter is a singleton shared by every test class in this Spring context.
    @BeforeEach
    @AfterEach
    void resetLimiter() {
        limiter.reset();
    }

    private ResultActions adminLogin(String username, String password) throws Exception {
        String body = """
                {"username":"%s","password":"%s"}
                """.formatted(username, password);
        return mockMvc.perform(post(ADMIN_LOGIN).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private void createBuyer(String username) {
        Role role = roleRepository.findByName(RoleName.BUYER_TENANT).orElseThrow();
        userRepository.save(User.builder()
                .username(username).email(username + "@sakan.com")
                .passwordHash(passwordEncoder.encode(BUYER_PASSWORD)).phone("+201012345678")
                .authProvider(AuthProvider.LOCAL).enabled(true)
                .roles(new HashSet<>(Set.of(role))).build());
    }

    private String detailOf(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.detail");
    }

    @Test
    @DisplayName("T15 seeded admin signs in and gets a JWT")
    void seededAdminSignsIn() throws Exception {
        adminLogin(adminUsername, adminPassword)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("T16 a valid non-admin user gets the same 401 as a wrong password")
    void nonAdminGets401() throws Exception {
        createBuyer("buyer1");

        ResultActions nonAdmin = adminLogin("buyer1", BUYER_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.token").doesNotExist());
        ResultActions wrongPassword = adminLogin(adminUsername, "WrongPass1")
                .andExpect(status().isUnauthorized());

        assertThat(detailOf(nonAdmin)).isEqualTo(detailOf(wrongPassword));
    }

    @Test
    @DisplayName("T17 the admin token is accepted by /api/auth/me and carries ADMIN")
    void adminTokenWorksOnMe() throws Exception {
        String json = adminLogin(adminUsername, adminPassword).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(json, "$.token");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(adminUsername))
                .andExpect(jsonPath("$.roles", hasItem("ADMIN")));
    }

    @Test
    @DisplayName("T20 no response body contains the password or a hash")
    void bodiesNeverContainSecrets() throws Exception {
        String success = adminLogin(adminUsername, adminPassword).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String failure = adminLogin(adminUsername, "WrongPass1").andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        for (String body : new String[] {success, failure}) {
            assertThat(body).doesNotContain(adminPassword).doesNotContain("$2a$").doesNotContain("$2b$")
                    .doesNotContain("passwordHash");
        }
    }

    @Test
    @DisplayName("T21 registering as ADMIN is still rejected with 400 (regression)")
    void registerAsAdminStillRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"username":"sneaky","email":"sneaky@sakan.com","password":"Passw0rd1",
                 "phone":"+201012345678","role":"ADMIN"}"""))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findByUsername("sneaky")).isEmpty();
    }

    @Test
    @DisplayName("E6 unknown user and wrong password return the same detail")
    void unknownUserSameAsWrongPassword() throws Exception {
        ResultActions unknown = adminLogin("ghost", "WrongPass1").andExpect(status().isUnauthorized());
        ResultActions wrong = adminLogin(adminUsername, "WrongPass1").andExpect(status().isUnauthorized());

        assertThat(detailOf(unknown)).isEqualTo(detailOf(wrong));
    }

    @Test
    @DisplayName("E7 empty body returns 400")
    void emptyBodyReturns400() throws Exception {
        mockMvc.perform(post(ADMIN_LOGIN).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}