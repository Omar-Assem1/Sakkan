package com.sakan.admin.service;

import com.sakan.admin.dto.AdminUserResponse;
import com.sakan.admin.dto.AdminUserSearchCriteria;
import com.sakan.common.exception.ForbiddenOperationException;
import com.sakan.common.exception.ResourceNotFoundException;
import com.sakan.common.exception.RoleConflictException;
import com.sakan.common.response.PageResponse;
import com.sakan.user.domain.RoleName;
import com.sakan.user.domain.User;
import com.sakan.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserServiceImpl implements AdminUserService {
    private final UserService userService;
    private final AdminUserMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public void assertAdmin(long actorId) {
        User actor = userService.findById(actorId).orElseThrow(() -> new ForbiddenOperationException("Administrator access required"));
        if (!actor.isEnabled() || actor.getRoles().stream().noneMatch(role -> role.getName() == RoleName.ADMIN))
            throw new ForbiddenOperationException("Administrator access required");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> search(AdminUserSearchCriteria criteria, Pageable pageable) {
        var page = userService.search(criteria.q(), criteria.role(), criteria.enabled(), pageable)
                .map(mapper::toResponse);
        return PageResponse.from(page);
    }

    @Override
    public AdminUserResponse grantRole(long actorId, long targetId, RoleName role) {
        User current = requireUser(targetId);
        if (current.getRoles().stream().anyMatch(r -> r.getName() == role)) return mapper.toResponse(current);
        return mapper.toResponse(userService.grantRole(targetId, role, actorId));
    }

    @Override
    public AdminUserResponse revokeRole(long actorId, long targetId, RoleName role) {
        User current = requireUser(targetId);
        if (current.isSeededAdmin()) throw new ForbiddenOperationException("The seeded admin cannot be demoted or changed");
        if (role == RoleName.ADMIN && actorId == targetId) throw new ForbiddenOperationException("An admin cannot demote themselves");
        if (current.getRoles().stream().noneMatch(r -> r.getName() == role)) return mapper.toResponse(current);
        if (current.getRoles().size() <= 1) throw new RoleConflictException("A user must keep at least one role");
        if (role == RoleName.ADMIN && userService.countByRole(RoleName.ADMIN) <= 1)
            throw new RoleConflictException("The last admin cannot be demoted");
        return mapper.toResponse(userService.revokeRole(targetId, role));
    }

    @Override
    @Transactional(readOnly = true)
    public void assertDeletable(long targetId) {
        if (requireUser(targetId).isSeededAdmin()) throw new ForbiddenOperationException("The seeded admin cannot be deleted");
    }

    private User requireUser(long id) {
        return userService.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
