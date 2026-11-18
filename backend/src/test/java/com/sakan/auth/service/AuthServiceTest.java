package com.sakan.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String RAW_PASSWORD = "Passw0rd1";
    private static final PasswordEncoder TEST_ENCODER = new BCryptPasswordEncoder(4);
    @Mock private UserService userService;
    @Mock private JwtService jwtService;
    @Spy private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
    @InjectMocks private AuthService authService;

    private RegisterRequest request(RoleName role) {
        return new RegisterRequest("omar", "omar@sakan.com", RAW_PASSWORD, "+201012345678", role);
    }

    private void stubCreateUser() {
        when(userService.createLocalUser(anyString(), anyString(), anyString(), anyString(), any(RoleName.class)))
                .thenAnswer(inv -> User.builder()
                        .id(1L)
                        .username(inv.getArgument(0))
                        .email(inv.getArgument(1))
                        .passwordHash(inv.getArgument(2))
                        .phone(inv.getArgument(3))
                        .authProvider(AuthProvider.LOCAL)
                        .enabled(true)
                        .roles(Set.of(Role.builder().id(1L).name(inv.getArgument(4)).build()))
                        .build());
    }

    private User localUser(boolean enabled) {
        return User.builder()
                .id(1L)
                .username("omar")
                .email("omar@sakan.com")
                .passwordHash(TEST_ENCODER.encode(RAW_PASSWORD))
                .authProvider(AuthProvider.LOCAL)
                .enabled(enabled)
                .roles(Set.of(Role.builder().id(1L).name(RoleName.BUYER_TENANT).build()))
                .build();
    }

    // ---------- Registration ----------

    // T01
    @Test
    void register_validRequest_returnsSummary() {
        stubCreateUser();

        UserSummaryResponse res = authService.register(request(RoleName.BUYER_TENANT));

        assertThat(res.id()).isEqualTo(1L);
        assertThat(res.username()).isEqualTo("omar");
        assertThat(res.email()).isEqualTo("omar@sakan.com");
        assertThat(res.roles()).containsExactly(RoleName.BUYER_TENANT);
    }

    // T01
    @Test
    void userSummary_hasNoPasswordComponent() {
        assertThat(Arrays.stream(UserSummaryResponse.class.getRecordComponents())
                .map(RecordComponent::getName))
                .noneMatch(n -> n.toLowerCase().contains("password"));
    }

    // T02
    @Test
    void register_duplicateUsername_throwsAndDoesNotCreate() {
        when(userService.existsByUsername("omar")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request(RoleName.OWNER)))
                .isInstanceOf(DuplicateUsernameException.class);
        verify(userService, never()).createLocalUser(any(), any(), any(), any(), any());
    }

    // T03
    @Test
    void register_duplicateEmail_throwsAndDoesNotCreate() {
        when(userService.existsByEmail("omar@sakan.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request(RoleName.OWNER)))
                .isInstanceOf(DuplicateEmailException.class);
        verify(userService, never()).createLocalUser(any(), any(), any(), any(), any());
    }

    // T04
    @ParameterizedTest
    @EnumSource(value = RoleName.class, names = {"BUYER_TENANT", "OWNER", "AGENT"})
    void register_passesRequestedRoleToUserService(RoleName role) {
        stubCreateUser();

        authService.register(request(role));

        ArgumentCaptor<RoleName> captor = ArgumentCaptor.forClass(RoleName.class);
        verify(userService).createLocalUser(anyString(), anyString(), anyString(), anyString(), captor.capture());
        assertThat(captor.getValue()).isEqualTo(role);
    }

    // T05, T06: role is checked before the database is touched
    @ParameterizedTest
    @EnumSource(value = RoleName.class, names = {"ADMIN", "MAINTENANCE_STAFF"})
    void register_privilegedRole_isRejectedBeforeAnyLookup(RoleName role) {
        assertThatThrownBy(() -> authService.register(request(role)))
                .isInstanceOf(RoleNotAllowedException.class);
        verifyNoInteractions(userService);
    }

    // T08
    @Test
    void register_storesBcryptHashNeverPlainPassword() {
        stubCreateUser();

        authService.register(request(RoleName.BUYER_TENANT));

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(userService).createLocalUser(anyString(), anyString(), hash.capture(), anyString(), any(RoleName.class));
        assertThat(hash.getValue()).startsWith("$2").isNotEqualTo(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, hash.getValue())).isTrue();
    }

    // ---------- Login ----------

    // T09
    @Test
    void login_validCredentials_returnsJwt() {
        User user = localUser(true);
        when(userService.findByUsername("omar")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        LoginResponse res = authService.login(new LoginRequest("omar", RAW_PASSWORD));

        assertThat(res.token()).isEqualTo("jwt-token");
        assertThat(res.tokenType()).isEqualTo("Bearer");
    }

    // T10
    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        when(userService.findByUsername("omar")).thenReturn(Optional.of(localUser(true)));

        assertThatThrownBy(() -> authService.login(new LoginRequest("omar", "WrongPass1")))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    // T11
    @Test
    void login_unknownUser_hasSameMessageAsWrongPassword() {
        when(userService.findByUsername("omar")).thenReturn(Optional.of(localUser(true)));
        when(userService.findByUsername("ghost")).thenReturn(Optional.empty());

        Throwable wrongPassword = catchThrowable(
                () -> authService.login(new LoginRequest("omar", "WrongPass1")));
        Throwable unknownUser = catchThrowable(
                () -> authService.login(new LoginRequest("ghost", RAW_PASSWORD)));

        assertThat(unknownUser).isInstanceOf(InvalidCredentialsException.class);
        assertThat(unknownUser.getMessage()).isEqualTo(wrongPassword.getMessage());
        verifyNoInteractions(jwtService);
    }

    // T12
    @Test
    void login_disabledAccount_throwsInvalidCredentials() {
        when(userService.findByUsername("omar")).thenReturn(Optional.of(localUser(false)));

        assertThatThrownBy(() -> authService.login(new LoginRequest("omar", RAW_PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(jwtService);
    }

    // E02: Google-only account has no password; password login must fail generically
    @Test
    void login_googleOnlyAccount_throwsInvalidCredentials() {
        User google = User.builder()
                .id(5L).username("g-user").email("g@sakan.com")
                .authProvider(AuthProvider.GOOGLE).enabled(true)
                .roles(Set.of(Role.builder().id(1L).name(RoleName.BUYER_TENANT).build()))
                .build();
        when(userService.findByUsername("g-user")).thenReturn(Optional.of(google));

        assertThatThrownBy(() -> authService.login(new LoginRequest("g-user", RAW_PASSWORD)))
                .isInstanceOf(InvalidCredentialsException.class);
        verifyNoInteractions(jwtService);
    }
}