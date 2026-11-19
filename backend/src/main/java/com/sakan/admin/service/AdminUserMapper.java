package com.sakan.admin.service;

import com.sakan.admin.dto.AdminUserResponse;
import com.sakan.user.domain.User;
import org.springframework.stereotype.Component;

@Component
public class AdminUserMapper {
    public AdminUserResponse toResponse(User user) { return AdminUserResponse.from(user); }
}
