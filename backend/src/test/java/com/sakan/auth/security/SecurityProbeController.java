package com.sakan.auth.security;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** TEST-ONLY. Delete when SCRUM-65 adds real role-protected endpoints. */
@RestController
@RequestMapping("/api/_probe")
class SecurityProbeController {

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    Map<String, String> admin(Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!admin) {
            throw new AccessDeniedException("Forbidden");
        }
        return Map.of("name", authentication.getName());
    }
}