package com.sakan.support.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Self-contained tests for {@link EndpointSecurityAssertions} and {@link Role}.
 *
 * <p>Uses a lightweight test-only {@link SecurityFilterChain} and controller so the helper is
 * proven for both positive cases and negative cases (wrong role expectations must fail with
 * {@link AssertionError}) without modifying production security configuration.
 */
@SpringJUnitWebConfig(EndpointSecurityAssertionsTest.TestSecurityConfig.class)
class EndpointSecurityAssertionsTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Role enum exposes all 5 domain roles and their ROLE_ authorities")
    void roleEnumHasFiveRolesWithAuthorityPrefix() {
        assertThat(Role.values()).containsExactly(
                Role.BUYER_TENANT,
                Role.OWNER,
                Role.AGENT,
                Role.MAINTENANCE_STAFF,
                Role.ADMIN);
        assertThat(Role.OWNER.authority()).isEqualTo("ROLE_OWNER");
        assertThat(Role.ADMIN.authority()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("assertUnauthorized passes when endpoint returns 401 and fails when endpoint is public (200)")
    void assertUnauthorizedDetectsCorrectAndWrongBehaviour() throws Exception {
        EndpointSecurityAssertions.assertUnauthorized(mvc, HttpMethod.POST, "/test-sec/listings");

        assertThatThrownBy(() ->
                EndpointSecurityAssertions.assertUnauthorized(mvc, HttpMethod.GET, "/test-sec/public"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected 401 Unauthorized");
    }

    @Test
    @DisplayName("assertForbiddenFor passes for forbidden roles (403) and fails if a role is actually allowed")
    void assertForbiddenForDetectsCorrectAndWrongBehaviour() throws Exception {
        EndpointSecurityAssertions.assertForbiddenFor(
                mvc,
                HttpMethod.POST,
                "/test-sec/listings",
                Role.BUYER_TENANT,
                Role.MAINTENANCE_STAFF,
                Role.ADMIN);

        assertThatThrownBy(() ->
                EndpointSecurityAssertions.assertForbiddenFor(
                        mvc, HttpMethod.POST, "/test-sec/listings", Role.OWNER))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected 403 Forbidden for role OWNER");
    }

    @Test
    @DisplayName("assertAllowedFor passes for permitted roles and fails if a role receives 403")
    void assertAllowedForDetectsCorrectAndWrongBehaviour() throws Exception {
        EndpointSecurityAssertions.assertAllowedFor(
                mvc,
                HttpMethod.POST,
                "/test-sec/listings",
                Role.OWNER,
                Role.AGENT);

        assertThatThrownBy(() ->
                EndpointSecurityAssertions.assertAllowedFor(
                        mvc, HttpMethod.POST, "/test-sec/listings", Role.BUYER_TENANT))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected role BUYER_TENANT to pass security");
    }

    @Test
    @DisplayName("assertAccessMatrix verifies 401 unauthenticated, 403 for forbidden roles, and allowed for permitted roles")
    void assertAccessMatrixVerifiesFullRoleMatrixAndFailsOnMismatch() throws Exception {
        EndpointSecurityAssertions.assertAccessMatrix(
                mvc,
                HttpMethod.POST,
                "/test-sec/listings",
                EnumSet.of(Role.OWNER, Role.AGENT));

        EndpointSecurityAssertions.assertAccessMatrix(
                mvc,
                HttpMethod.DELETE,
                "/test-sec/admin-only",
                Set.of(Role.ADMIN));

        // Negative check: passing the wrong allowedRoles set must fail the assertion
        assertThatThrownBy(() ->
                EndpointSecurityAssertions.assertAccessMatrix(
                        mvc,
                        HttpMethod.DELETE,
                        "/test-sec/admin-only",
                        Set.of(Role.OWNER)))
                .isInstanceOf(AssertionError.class);
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/test-sec/public").permitAll()
                            .requestMatchers(HttpMethod.POST, "/test-sec/listings")
                            .hasAnyRole(Role.OWNER.name(), Role.AGENT.name())
                            .requestMatchers(HttpMethod.DELETE, "/test-sec/admin-only")
                            .hasRole(Role.ADMIN.name())
                            .anyRequest().authenticated())
                    .exceptionHandling(ex -> ex
                            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                    .build();
        }

        @Bean
        TestSecuredController testSecuredController() {
            return new TestSecuredController();
        }
    }

    @RestController
    static class TestSecuredController {

        @GetMapping("/test-sec/public")
        String publicEndpoint() {
            return "ok";
        }

        @PostMapping("/test-sec/listings")
        String manageListings() {
            return "created";
        }

        @DeleteMapping("/test-sec/admin-only")
        String adminOnly() {
            return "deleted";
        }
    }
}
