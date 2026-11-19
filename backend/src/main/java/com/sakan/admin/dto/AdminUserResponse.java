package com.sakan.admin.dto;

import com.sakan.user.domain.User;
import java.util.List;

public record AdminUserResponse(Long id, String username, String email, String phone,
        List<String> roles, boolean enabled, boolean seededAdmin, String authProvider) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getPhone(),
                user.getRoles().stream().map(r -> r.getName().name()).sorted().toList(), user.isEnabled(),
                user.isSeededAdmin(), user.getAuthProvider().name());
    }
}
