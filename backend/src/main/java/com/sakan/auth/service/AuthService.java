
package com.sakan.auth.service;

import com.sakan.auth.dto.LoginRequest;
import com.sakan.auth.dto.LoginResponse;
import com.sakan.auth.dto.RegisterRequest;
import com.sakan.auth.dto.UserSummaryResponse;
import com.sakan.common.exception.DuplicateEmailException;
import com.sakan.common.exception.DuplicateUsernameException;
import com.sakan.common.exception.InvalidCredentialsException;
import com.sakan.common.exception.RoleNotAllowedException;
import com.sakan.user.domain.AuthProvider;
import com.sakan.user.domain.Role;
import com.sakan.user.domain.RoleName;
import com.sakan.user.domain.User;
import com.sakan.user.service.UserService;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    /** Roles a visitor may pick. ADMIN and MAINTENANCE_STAFF are granted only by an admin. */
    private static final Set<RoleName> SELF_SERVICE_ROLES =
            EnumSet.of(RoleName.BUYER_TENANT, RoleName.OWNER, RoleName.AGENT);

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /** Used to spend the same time on unknown users as on known ones. */
    private final String dummyHash;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public UserSummaryResponse register(RegisterRequest request) {
        if (!SELF_SERVICE_ROLES.contains(request.role())) {
            throw new RoleNotAllowedException();   // checked first: no database access for escalation attempts
        }
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userService.existsByUsername(request.username())) {
            throw new DuplicateUsernameException();
        }
        if (userService.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }
        User user = userService.createLocalUser(request.username(), email,
                passwordEncoder.encode(request.password()), request.phone(), request.role());
        return toSummary(user);
    }

    public LoginResponse login(LoginRequest request) {
        return authenticate(request, false);
    }

    /** Admin sign-in (form 2): same checks as login, plus the ADMIN role. Any failure gives the same 401. */
    public LoginResponse adminLogin(LoginRequest request) {
        return authenticate(request, true);
    }

    private LoginResponse authenticate(LoginRequest request, boolean adminOnly) {
        User user = userService.findByUsername(request.username()).orElse(null);
        String hash = (user != null && user.getPasswordHash() != null) ? user.getPasswordHash() : dummyHash;
        boolean passwordOk = passwordEncoder.matches(request.password(), hash);

        boolean roleOk = !adminOnly || (user != null && user.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ADMIN));

        if (user == null || !passwordOk || !user.isEnabled()
                || user.getAuthProvider() != AuthProvider.LOCAL || !roleOk) {
            throw new InvalidCredentialsException();
        }
        return LoginResponse.bearer(jwtService.generateToken(user));
    }   

    public UserSummaryResponse currentUser(Long userId) {
        return userService.findById(userId)
                .filter(User::isEnabled)
                .map(AuthService::toSummary)
                .orElseThrow(InvalidCredentialsException::new);
    }

    private static UserSummaryResponse toSummary(User user) {
        Set<RoleName> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return new UserSummaryResponse(user.getId(), user.getUsername(), user.getEmail(), user.getPhone(), roles);
    }
}