package com.sakan.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GoogleRegistrationRequest(
        @NotBlank @Size(max = 128) String code,
        @NotBlank @Size(min = 3, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "may contain only letters, digits, dot, dash and underscore")
        String username) {
}
