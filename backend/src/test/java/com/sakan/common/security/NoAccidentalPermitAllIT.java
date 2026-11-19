package com.sakan.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.sakan.support.AbstractIntegrationTest;
import com.sakan.support.tags.RegressionTest;

/**
 * SCRUM-65: no endpoint may be public by accident. The PUBLIC set is the only place where an
 * open endpoint is allowed; adding one is a deliberate, reviewed change.
 */
@RegressionTest
@AutoConfigureMockMvc
class NoAccidentalPermitAllIT extends AbstractIntegrationTest {

    /** Normalized as "METHOD /path/{}" (path variable names removed). */
    private static final Set<String> PUBLIC = Set.of(
            "POST /api/auth/register",
            "POST /api/auth/login",
            "POST /api/auth/admin/login",
            "GET /api/properties",
            "GET /api/properties/{}",
            "GET /api/properties/{}/slots",
            "GET /api/properties/{}/images",
            "GET /api/property-images/{}",
            "GET /api/locations/governorates",
            "GET /api/locations/cities",
            "GET /api/locations/districts",
            "GET /api/locations/compounds");

    private static final List<String> SKIPPED_PREFIXES = List.of("/error", "/v3/api-docs", "/swagger-ui");

    @Autowired
    private MockMvc mvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mapping;

    private record Endpoint(String method, String pattern, HandlerMethod handler) {
        String key() {
            return method + " " + pattern.replaceAll("\\{[^}/]+}", "{}");
        }

        String concreteUrl() {
            return pattern.replaceAll("\\{[^}/]+}", "1");
        }
    }

    private List<Endpoint> endpoints() {
        List<Endpoint> result = new ArrayList<>();
        mapping.getHandlerMethods().forEach((info, handler) -> {
            Set<String> patterns = info.getPathPatternsCondition() == null
                    ? Set.of() : info.getPathPatternsCondition().getPatternValues();
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            for (String pattern : patterns) {
                if (SKIPPED_PREFIXES.stream().anyMatch(pattern::startsWith)) {
                    continue;
                }
                if (methods.isEmpty()) {
                    result.add(new Endpoint("GET", pattern, handler));
                }
                for (RequestMethod m : methods) {
                    result.add(new Endpoint(m.name(), pattern, handler));
                }
            }
        });
        return result;
    }

    @Test
    @DisplayName("Every endpoint answers 401 to an anonymous caller unless it is on the explicit public list")
    void everyNonPublicEndpointRequiresAuthentication() throws Exception {
        List<String> leaks = new ArrayList<>();
        for (Endpoint e : endpoints()) {
            if (PUBLIC.contains(e.key())) {
                continue;
            }
            int status = mvc.perform(request(HttpMethod.valueOf(e.method()), e.concreteUrl()))
                    .andReturn().getResponse().getStatus();
            if (status != 401) {
                leaks.add(e.key() + " -> " + status);
            }
        }
        assertThat(leaks).as("Endpoints reachable without a token").isEmpty();
    }

    @Test
    @DisplayName("Every non-public endpoint carries @PreAuthorize on the method or controller")
    void everyNonPublicEndpointHasMethodSecurity() {
        List<String> missing = new ArrayList<>();
        for (Endpoint e : endpoints()) {
            if (PUBLIC.contains(e.key())) {
                continue;
            }
            boolean annotated =
                    AnnotatedElementUtils.findMergedAnnotation(e.handler().getMethod(), PreAuthorize.class) != null
                            || AnnotatedElementUtils.findMergedAnnotation(e.handler().getBeanType(), PreAuthorize.class) != null;
            if (!annotated) {
                missing.add(e.key());
            }
        }
        assertThat(missing).as("Endpoints without @PreAuthorize").isEmpty();
    }

    @Test
    @DisplayName("The public list has no stale entries and each public endpoint really is open")
    void publicListMatchesReality() throws Exception {
        List<Endpoint> all = endpoints();
        List<String> keys = all.stream().map(Endpoint::key).toList();
        assertThat(keys).as("PUBLIC entries that no controller maps").containsAll(PUBLIC);

        for (Endpoint e : all) {
            if (!PUBLIC.contains(e.key())) {
                continue;
            }
            int status = mvc.perform(request(HttpMethod.valueOf(e.method()), e.concreteUrl()))
                    .andReturn().getResponse().getStatus();
            assertThat(status).as("public %s", e.key()).isNotIn(401, 403);
        }
    }

    @ParameterizedTest(name = "unmapped path {0} is denied by default")
    @ValueSource(strings = {
            "/api/does-not-exist",
            "/api/properties/1/unknown",
            "/api/properties/1/offers",
            "/api/properties/1/images/extra/deep",
            "/api/admin/users"})
    @DisplayName("Unknown paths fall to authenticated(), never to permitAll()")
    void unknownPathsAreDeniedByDefault(String path) throws Exception {
        int status = mvc.perform(get(path)).andReturn().getResponse().getStatus();
        assertThat(status).isEqualTo(401);
    }
}
