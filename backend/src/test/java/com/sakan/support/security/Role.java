package com.sakan.support.security;

/**
 * The five application roles defined in the Sakan domain model.
 *
 * <p>Use with {@link EndpointSecurityAssertions} in {@code MockMvc} RBAC tests to verify
 * which roles are allowed ({@code != 401/403}) or forbidden ({@code 403}) on an endpoint.
 */
public enum Role {
    BUYER_TENANT,
    OWNER,
    AGENT,
    MAINTENANCE_STAFF,
    ADMIN;

    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Returns the Spring Security granted-authority string (for example {@code "ROLE_OWNER"}).
     */
    public String authority() {
        return ROLE_PREFIX + name();
    }
}
