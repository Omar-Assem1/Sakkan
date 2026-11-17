package com.sakan.common.security;

import java.util.Set;

public record AuthenticatedUser(Long id, Set<String> roles) {

    public static final String ADMIN = "ADMIN";
    public static final String OWNER = "OWNER";
    public static final String AGENT = "AGENT";

    public AuthenticatedUser {
        roles = Set.copyOf(roles);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }

    /** Owners and agents may create and manage property listings (FR-13). */
    public boolean canManageListings() {
        return hasRole(OWNER) || hasRole(AGENT);
    }
}
