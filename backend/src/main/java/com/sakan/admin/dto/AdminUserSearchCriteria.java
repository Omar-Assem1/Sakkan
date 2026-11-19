package com.sakan.admin.dto;

import com.sakan.user.domain.RoleName;

public record AdminUserSearchCriteria(String q, RoleName role, Boolean enabled) {}
