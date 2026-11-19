package com.sakan.admin.service;

import com.sakan.admin.dto.AdminUserSearchCriteria;
import com.sakan.admin.dto.AdminUserResponse;
import com.sakan.common.response.PageResponse;
import com.sakan.user.domain.RoleName;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    void assertAdmin(long actorId);
    PageResponse<AdminUserResponse> search(AdminUserSearchCriteria criteria, Pageable pageable);
    AdminUserResponse grantRole(long actorId, long targetId, RoleName role);
    AdminUserResponse revokeRole(long actorId, long targetId, RoleName role);
    void assertDeletable(long targetId);
}
