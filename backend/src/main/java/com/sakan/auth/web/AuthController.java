package com.sakan.auth.web;

import com.sakan.auth.dto.LoginRequest;
import com.sakan.auth.dto.LoginResponse;
import com.sakan.auth.dto.RegisterRequest;
import com.sakan.auth.dto.UserSummaryResponse;
import com.sakan.auth.service.AuthService;
import com.sakan.common.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/register")
    @Operation(summary = "Register as BUYER_TENANT, OWNER or AGENT (no token is returned)")
    public ResponseEntity<UserSummaryResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with username and password, returns a JWT (HS256, 24 h)")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Current user, from the JWT")
    public UserSummaryResponse me() {
        return authService.currentUser(currentUserProvider.require().id());
    }
}