package com.sakan.admin.web;

import static com.sakan.support.security.EndpointSecurityAssertions.assertForbiddenFor;
import static com.sakan.support.security.EndpointSecurityAssertions.assertUnauthorized;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;

import com.sakan.support.AbstractIntegrationTest;
import com.sakan.support.security.Role;
import com.sakan.support.tags.RegressionTest;

/**
 * SCRUM-69: every admin endpoint returns 401 without a token and 403 for the four non-admin roles.
 * The ADMIN-allowed side lives in AdminUserControllerIT, because admin access is also checked in the DB.
 */
@RegressionTest
@AutoConfigureMockMvc
class AdminEndpointAccessIT extends AbstractIntegrationTest {

    @Autowired private MockMvc mvc;

    static Stream<Arguments> adminEndpoints() {
        return Stream.of(
                Arguments.of(HttpMethod.GET, "/api/admin/users"),
                Arguments.of(HttpMethod.POST, "/api/admin/users/1/roles"),
                Arguments.of(HttpMethod.DELETE, "/api/admin/users/1/roles/OWNER"));
    }

    @ParameterizedTest(name = "{0} {1}")
    @MethodSource("adminEndpoints")
    @DisplayName("401 without token, 403 for every non-admin role")
    void nonAdminsAreRejected(HttpMethod method, String url) throws Exception {
        assertUnauthorized(mvc, method, url);
        assertForbiddenFor(mvc, method, url,
                Role.BUYER_TENANT, Role.OWNER, Role.AGENT, Role.MAINTENANCE_STAFF);
    }
}
