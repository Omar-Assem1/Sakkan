package com.sakan.auth.dto;

import com.sakan.user.domain.RoleName;
import java.util.Set;

public record UserSummaryResponse(Long id, String username, String email, String phone, Set<RoleName> roles) {
}