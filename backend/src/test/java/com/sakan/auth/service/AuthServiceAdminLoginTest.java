package com.sakan.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sakan.auth.dto.LoginRequest;
import com.sakan.auth.dto.LoginResponse;
import com.sakan.common.exception.InvalidCredentialsException;
import com.sakan.support.tags.RegressionTest;
import com.sakan.user.domain.AuthProvider;
import com.sakan.user.domain.Role;
import com.sakan.user.domain.RoleName;
import com.sakan.user.domain.User;
import com.sakan.user.service.UserService;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@RegressionTest
@ExtendWith(MockitoExtension.class)
class AuthServiceAdminLoginTest {

    private static final String USERNAME = "admin1";
    private static final String PASSWORD = "Adm1nPassw0rd";

    @Mock private UserService userService;
    @Mock private JwtService jwtService;
    private final PasswordEncoder encoder = spy(new BCryptPasswordEncoder(4));

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userService, encoder, jwtService);
    }

    private User user(boolean enabled, AuthProvider provider, String hash, RoleName... roles) {
        Set<Role> roleSet = new HashSet<>();
        for (RoleName r : roles) {
            roleSet.add(Role.builder().name(r).build());
        }
        return User.builder().id(1L).username(USERNAME).email("admin1@sakan.test")
                .passwordHash(hash).authProvider(provider).enabled(enabled).roles(roleSet).build();
    }

    private LoginRequest request(String password) {
        return new LoginRequest(USERNAME, password);
    }

    @Test
    @DisplayName("T07 valid admin credentials return a JWT")
    void validAdminGetsToken() {
        User admin = user(true, AuthProvider.LOCAL, encoder.encode(PASSWORD), RoleName.ADMIN);
        when(userService.findByUsername(USERNAME)).thenReturn(Optional.of(admin));
        when(jwtService.generateToken(admin)).thenReturn("jwt-token");

        assertThat(authService.adminLogin(request(PASSWORD))).isEqualTo(LoginResponse.bearer("jwt-token"));
    }

    @Test
    @DisplayName("T08 wrong password is rejected")
    void wrongPasswordRejected() {
        User admin = user(true, AuthProvider.LOCAL, encoder.encode(PASSWORD), RoleName.ADMIN);
        when(userService.findByUsername(USERNAME)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.adminLogin(request("WrongPass1")))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("T09 valid credentials without the ADMIN role are rejected like a wrong password")
    void nonAdminRejected() {
        User buyer = user(true, AuthProvider.LOCAL, encoder.encode(PASSWORD), RoleName.BUYER_TENANT);
        when(userService.findByUsername(USERNAME)).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> authService.adminLogin(request(PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("T10 unknown user is rejected and still spends a password check")
    void unknownUserRejectedWithSameWork() {
        when(userService.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.adminLogin(new LoginRequest("ghost", PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(encoder).matches(eq(PASSWORD), anyString());
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("T11 disabled admin is rejected")
    void disabledAdminRejected() {
        User admin = user(false, AuthProvider.LOCAL, encoder.encode(PASSWORD), RoleName.ADMIN);
        when(userService.findByUsername(USERNAME)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.adminLogin(request(PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("E4 an admin who also holds another role can still sign in")
    void adminWithExtraRoleCanSignIn() {
        User admin = user(true, AuthProvider.LOCAL, encoder.encode(PASSWORD), RoleName.ADMIN, RoleName.OWNER);
        when(userService.findByUsername(USERNAME)).thenReturn(Optional.of(admin));
        when(jwtService.generateToken(admin)).thenReturn("jwt-token");

        assertThat(authService.adminLogin(request(PASSWORD))).isEqualTo(LoginResponse.bearer("jwt-token"));
    }

    @Test
    @DisplayName("E5 a Google account without a password cannot use the admin form")
    void googleAccountRejected() {
        User google = user(true, AuthProvider.GOOGLE, null, RoleName.ADMIN);
        when(userService.findByUsername(USERNAME)).thenReturn(Optional.of(google));

        assertThatThrownBy(() -> authService.adminLogin(request(PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(jwtService);
    }
}