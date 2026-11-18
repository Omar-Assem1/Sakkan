package com.sakan.auth.dto;

import com.sakan.user.domain.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "may contain only letters, digits, dot, dash and underscore")
        String username,

        @NotBlank @Email @Size(max = 255)
        String email,

        @NotBlank @Size(min = 8, max = 72, message = "must be 8 to 72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "must contain at least one letter and one digit")
        String password,

        @NotBlank
        @Pattern(regexp = "^\\+[1-9]\\d{1,14}$", message = "must be in E.164 format, for example +201012345678")
        String phone,

        @NotNull
        RoleName role) {
}