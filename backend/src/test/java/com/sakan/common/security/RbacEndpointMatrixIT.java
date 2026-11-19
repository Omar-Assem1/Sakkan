package com.sakan.common.security;

import static com.sakan.support.security.EndpointSecurityAssertions.assertAccessMatrix;
import static com.sakan.support.security.EndpointSecurityAssertions.assertAllowedFor;
import static com.sakan.support.security.EndpointSecurityAssertions.assertUnauthorized;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.sakan.support.AbstractIntegrationTest;
import com.sakan.support.security.EndpointSecurityAssertions;
import com.sakan.support.security.Role;
import com.sakan.support.tags.RegressionTest;

/** SCRUM-65: 401 without token, 403 with the wrong role, not 401/403 with an allowed role. */
@RegressionTest
@AutoConfigureMockMvc
class RbacEndpointMatrixIT extends AbstractIntegrationTest {

    private static final Set<Role> LISTING_MANAGERS = EnumSet.of(Role.OWNER, Role.AGENT);
    private static final Set<Role> VIEWING_REQUESTERS = EnumSet.of(Role.BUYER_TENANT, Role.OWNER, Role.AGENT);
    private static final Set<Role> ADMIN_ONLY = EnumSet.of(Role.ADMIN);

    @Autowired
    private MockMvc mvc;

    static Stream<Arguments> protectedEndpoints() {
        return Stream.of(
                // group: property listing management
                Arguments.of("property-management", HttpMethod.GET, "/api/properties/mine", LISTING_MANAGERS),
                Arguments.of("property-management", HttpMethod.POST, "/api/properties", LISTING_MANAGERS),
                Arguments.of("property-management", HttpMethod.PUT, "/api/properties/999999", LISTING_MANAGERS),
                Arguments.of("property-management", HttpMethod.POST, "/api/properties/999999/publish", LISTING_MANAGERS),
                Arguments.of("property-management", HttpMethod.POST, "/api/properties/999999/archive", LISTING_MANAGERS),
                // group: viewing slots
                Arguments.of("viewing-slots", HttpMethod.POST, "/api/properties/999999/slots", LISTING_MANAGERS),
                Arguments.of("viewing-slots", HttpMethod.DELETE, "/api/properties/999999/slots/1", LISTING_MANAGERS),
                // group: property images
                Arguments.of("property-images", HttpMethod.POST, "/api/properties/999999/images", LISTING_MANAGERS),
                Arguments.of("property-images", HttpMethod.PUT, "/api/properties/999999/images/order", LISTING_MANAGERS),
                Arguments.of("property-images", HttpMethod.DELETE, "/api/properties/999999/images/1", LISTING_MANAGERS),
                // group: viewings
                Arguments.of("viewings", HttpMethod.POST, "/api/viewings", VIEWING_REQUESTERS),
                // group: operations (actuator beyond health/info)
                Arguments.of("operations", HttpMethod.GET, "/actuator/env", ADMIN_ONLY),
                Arguments.of("operations", HttpMethod.GET, "/actuator/metrics", ADMIN_ONLY));
    }

    @ParameterizedTest(name = "[{0}] {1} {2}")
    @MethodSource("protectedEndpoints")
    @DisplayName("401 without token, 403 for every other role, allowed for the listed roles")
    void accessMatrix(String group, HttpMethod method, String url, Set<Role> allowed) throws Exception {
        assertAccessMatrix(mvc, method, url, allowed);
    }

    @Test
    @DisplayName("GET /api/auth/me: 401 without token (any authenticated role is checked in JwtTamperingSecurityIT)")
    void meRequiresAuthentication() throws Exception {
        assertUnauthorized(mvc, HttpMethod.GET, "/api/auth/me");
    }

    static Stream<Arguments> publicEndpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/properties"),
                Arguments.of(HttpMethod.GET, "/api/properties/999999"),
                Arguments.of(HttpMethod.GET, "/api/properties/999999/slots"),
                Arguments.of(HttpMethod.GET, "/api/properties/999999/images"),
                Arguments.of(HttpMethod.GET, "/api/property-images/does-not-exist.jpg"),
                Arguments.of(HttpMethod.GET, "/v3/api-docs"),
                Arguments.of(HttpMethod.GET, "/actuator/health"),
                Arguments.of(HttpMethod.POST, "/api/auth/register"),
                Arguments.of(HttpMethod.POST, "/api/auth/login"),
                Arguments.of(HttpMethod.POST, "/api/auth/admin/login"));
    }

    @ParameterizedTest(name = "public: {0} {1}")
    @MethodSource("publicEndpoints")
    @DisplayName("Public endpoints never answer 401/403, anonymous or with any role")
    void publicEndpointsAreOpen(HttpMethod method, String url) throws Exception {
        int anonymous = mvc.perform(request(method, url).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();
        assertThat(anonymous).as("anonymous %s %s", method, url).isNotIn(401, 403);

        assertAllowedFor(mvc, method, url, Role.values());
    }

    @Test
    @DisplayName("401 is problem+json")
    void unauthorizedIsProblemJson() throws Exception {
        mvc.perform(request(HttpMethod.POST, "/api/properties").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("403 is problem+json and is returned before body validation")
    void forbiddenIsProblemJsonBeforeValidation() throws Exception {
        mvc.perform(request(HttpMethod.POST, "/api/properties")
                        .with(EndpointSecurityAssertions.mockUser(Role.BUYER_TENANT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403));
    }
}
