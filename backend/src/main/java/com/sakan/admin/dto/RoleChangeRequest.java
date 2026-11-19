package com.sakan.admin.dto;

import com.sakan.user.domain.RoleName;
import jakarta.validation.constraints.NotNull;

public record RoleChangeRequest(@NotNull RoleName role) {}
