package com.dwtd.myanimelist.features.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 4, max = 24, message = "Username must be between {min} and {max} characters")
        String username,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least {min} characters")
        @Pattern(regexp = "\\S+", message = "Password must not contain spaces")
        String password
) {
}