package com.sakan.auth.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sakan.support.AbstractIntegrationTest;
import com.sakan.user.domain.AuthProvider;
import com.sakan.user.domain.Role;
import com.sakan.user.domain.RoleName;
import com.sakan.user.domain.User;
import com.sakan.user.repository.RoleRepository;
import com.sakan.user.repository.UserRepository;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "app.security.login-rate-limit.max-attempts=5",
        "app.security.login-rate-limit.window-minutes=15"})
class LoginRateLimitIT extends AbstractIntegrationTest {

    private static final String PASSWORD = "Passw0rd1";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private LoginAttemptLimiter limiter;

    @BeforeEach
    void clean() {
        limiter.reset();
        Role role = roleRepository.findByName(RoleName.BUYER_TENANT).orElseThrow();
        userRepository.save(User.builder()
                .username("omar").email("omar@sakan.com")
                .passwordHash(passwordEncoder.encode(PASSWORD)).phone("+201012345678")
                .authProvider(AuthProvider.LOCAL).enabled(true)
                .roles(new HashSet<>(Set.of(role))).build());
    }

    private static RequestPostProcessor ip(String addr) {
        return req -> {
            req.setRemoteAddr(addr);
            return req;
        };
    }

    private org.springframework.test.web.servlet.ResultActions login(String password, String addr) throws Exception {
        return mockMvc.perform(post("/api/auth/login").with(ip(addr)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"omar\",\"password\":\"%s\"}".formatted(password)));
    }

    private void fail(int times, String addr) throws Exception {
        for (int i = 0; i < times; i++) {
            login("WrongPass1", addr).andExpect(status().isUnauthorized());
        }
    }

    @Test @DisplayName("R01 6th attempt after 5 failures returns 429 problem+json with Retry-After")
    void blocksAfterMaxFailures() throws Exception {
        fail(5, "10.0.0.1");
        login("WrongPass1", "10.0.0.1")
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test @DisplayName("R02 correct password is also blocked while locked out")
    void correctPasswordBlockedWhileLocked() throws Exception {
        fail(5, "10.0.0.2");
        login(PASSWORD, "10.0.0.2").andExpect(status().isTooManyRequests());
    }

    @Test @DisplayName("R03 another client IP is not affected")
    void otherIpNotBlocked() throws Exception {
        fail(5, "10.0.0.3");
        login(PASSWORD, "10.0.0.4").andExpect(status().isOk());
    }

    @Test @DisplayName("R04 a successful login resets the failure counter")
    void successResetsCounter() throws Exception {
        fail(4, "10.0.0.5");
        login(PASSWORD, "10.0.0.5").andExpect(status().isOk());
        fail(4, "10.0.0.5");
        login(PASSWORD, "10.0.0.5").andExpect(status().isOk());
    }
}