package com.sakan.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleCodeRequest(@NotBlank @Size(max = 128) String code) {
}
