package com.sakan.admin.web;

import com.sakan.admin.dto.AdminUserSearchCriteria;
import com.sakan.admin.dto.AdminUserResponse;
import com.sakan.admin.dto.RoleChangeRequest;
import com.sakan.admin.service.AdminUserService;
import com.sakan.common.response.PageResponse;
import com.sakan.common.security.CurrentUserProvider;
import com.sakan.user.domain.RoleName;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin users")
public class AdminUserController {
    private final AdminUserService service;
    private final CurrentUserProvider currentUser;

    public AdminUserController(AdminUserService service, CurrentUserProvider currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(summary = "List users with optional filters and pagination")
    public ResponseEntity<PageResponse<AdminUserResponse>> list(@RequestParam(required = false) String q,
            @RequestParam(required = false) RoleName role, @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "username,asc") String sort) {
        service.assertAdmin(currentUser.require().id());
        if (page < 0 || size < 1 || size > 100) throw new IllegalArgumentException("Invalid page or size");
        String[] sortParts = sort.split(",", 2);
        String field = switch (sortParts[0]) { case "username", "email", "id" -> sortParts[0]; default -> throw new IllegalArgumentException("Invalid sort field"); };
        Sort.Direction direction = sortParts.length > 1 ? Sort.Direction.fromString(sortParts[1]) : Sort.Direction.ASC;
        return ResponseEntity.ok(service.search(new AdminUserSearchCriteria(q, role, enabled),
                PageRequest.of(page, size, Sort.by(direction, field))));
    }

    @PostMapping("/{id}/roles")
    @Operation(summary = "Grant an additional role to a user")
    public ResponseEntity<AdminUserResponse> grant(@PathVariable long id, @Valid @RequestBody RoleChangeRequest request) {
        long actorId = currentUser.require().id();
        service.assertAdmin(actorId);
        return ResponseEntity.ok(service.grantRole(actorId, id, request.role()));
    }

    @DeleteMapping("/{id}/roles/{role}")
    @Operation(summary = "Revoke a role from a user")
    public ResponseEntity<AdminUserResponse> revoke(@PathVariable long id, @PathVariable RoleName role) {
        long actorId = currentUser.require().id();
        service.assertAdmin(actorId);
        return ResponseEntity.ok(service.revokeRole(actorId, id, role));
    }
}
