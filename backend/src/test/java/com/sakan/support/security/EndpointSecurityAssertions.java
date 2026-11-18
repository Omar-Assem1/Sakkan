package com.sakan.support.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.sakan.common.security.SecurityContextCurrentUserProvider;

/**
 * Reusable {@link MockMvc} assertions for endpoint authentication ({@code 401 Unauthorized})
 * and role-based access control ({@code 403 Forbidden}).
 *
 * <p>Uses {@code SecurityMockMvcRequestPostProcessors.user("1").roles(role.name())} with a
 * numeric username so {@link SecurityContextCurrentUserProvider} can also parse the user ID
 * when controllers call {@code currentUserProvider.require()}.
 *
 * <p><b>Example usage:</b>
 * <pre>{@code
 * // 1. Assert a single rule:
 * EndpointSecurityAssertions.assertUnauthorized(mvc, HttpMethod.POST, "/api/properties");
 * EndpointSecurityAssertions.assertForbiddenFor(mvc, HttpMethod.POST, "/api/properties",
 *         Role.BUYER_TENANT, Role.MAINTENANCE_STAFF);
 * EndpointSecurityAssertions.assertAllowedFor(mvc, HttpMethod.POST, "/api/properties",
 *         Role.OWNER, Role.AGENT);
 *
 * // 2. Or check the full 5-role access matrix + unauthenticated 401 in one call:
 * EndpointSecurityAssertions.assertAccessMatrix(
 *         mvc, HttpMethod.DELETE, "/api/admin/users/42", EnumSet.of(Role.ADMIN));
 * }</pre>
 */
public final class EndpointSecurityAssertions {

    private static final String DEFAULT_TEST_USER_ID = "1";

    private EndpointSecurityAssertions() {
    }

    /**
     * Asserts that an unauthenticated request to {@code method url} returns {@code 401 Unauthorized}.
     */
    public static void assertUnauthorized(MockMvc mvc, HttpMethod method, String url) throws Exception {
        Objects.requireNonNull(mvc, "mvc must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(url, "url must not be null");

        MvcResult result = mvc.perform(buildRequest(method, url)).andReturn();
        int status = result.getResponse().getStatus();

        assertThat(status)
                .as("Expected 401 Unauthorized for unauthenticated %s %s, but got %d", method, url, status)
                .isEqualTo(HttpStatus.UNAUTHORIZED.value());
    }

    /**
     * Asserts that an authenticated request returns {@code 403 Forbidden} for every given role.
     */
    public static void assertForbiddenFor(MockMvc mvc, HttpMethod method, String url, Role... roles) throws Exception {
        Objects.requireNonNull(mvc, "mvc must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(url, "url must not be null");
        Objects.requireNonNull(roles, "roles must not be null");
        assertThat(roles).as("At least one role must be provided to assertForbiddenFor").isNotEmpty();

        for (Role role : roles) {
            MvcResult result = mvc.perform(buildRequest(method, url).with(mockUser(role))).andReturn();
            int status = result.getResponse().getStatus();

            assertThat(status)
                    .as("Expected 403 Forbidden for role %s on %s %s, but got %d", role, method, url, status)
                    .isEqualTo(HttpStatus.FORBIDDEN.value());
        }
    }

    /**
     * Asserts that an authenticated request is NOT rejected by security (status is neither {@code 401}
     * nor {@code 403}) for every given role.
     */
    public static void assertAllowedFor(MockMvc mvc, HttpMethod method, String url, Role... roles) throws Exception {
        Objects.requireNonNull(mvc, "mvc must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(url, "url must not be null");
        Objects.requireNonNull(roles, "roles must not be null");
        assertThat(roles).as("At least one role must be provided to assertAllowedFor").isNotEmpty();

        for (Role role : roles) {
            MvcResult result = mvc.perform(buildRequest(method, url).with(mockUser(role))).andReturn();
            int status = result.getResponse().getStatus();

            assertThat(status)
                    .as("Expected role %s to pass security (not 401/403) on %s %s, but got %d",
                            role, method, url, status)
                    .isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());
        }
    }

    /**
     * Verifies the complete RBAC matrix for {@code method url}:
     * <ul>
     *   <li>Unauthenticated request returns {@code 401 Unauthorized}.</li>
     *   <li>Every role in {@link Role#values()} NOT in {@code allowedRoles} returns {@code 403 Forbidden}.</li>
     *   <li>Every role in {@code allowedRoles} returns a status that is neither {@code 401} nor {@code 403}.</li>
     * </ul>
     */
    public static void assertAccessMatrix(
            MockMvc mvc,
            HttpMethod method,
            String url,
            Set<Role> allowedRoles) throws Exception {
        Objects.requireNonNull(allowedRoles, "allowedRoles must not be null");

        assertUnauthorized(mvc, method, url);

        Set<Role> forbiddenRoles = EnumSet.allOf(Role.class);
        forbiddenRoles.removeAll(allowedRoles);

        if (!forbiddenRoles.isEmpty()) {
            assertForbiddenFor(mvc, method, url, forbiddenRoles.toArray(Role[]::new));
        }
        if (!allowedRoles.isEmpty()) {
            assertAllowedFor(mvc, method, url, allowedRoles.toArray(Role[]::new));
        }
    }

    /**
     * Creates a Spring Security {@link UserRequestPostProcessor} with numeric principal ID {@code "1"}
     * and the given {@link Role}.
     */
    public static UserRequestPostProcessor mockUser(Role role) {
        Objects.requireNonNull(role, "role must not be null");
        return user(DEFAULT_TEST_USER_ID).roles(role.name());
    }

    private static MockHttpServletRequestBuilder buildRequest(HttpMethod method, String url) {
        return MockMvcRequestBuilders.request(method, url)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{}");
    }
}
