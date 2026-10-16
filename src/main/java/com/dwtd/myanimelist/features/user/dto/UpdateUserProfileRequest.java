package com.dwtd.myanimelist.features.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(min = 4, max = 24, message = "Username must be between {min} and {max} characters")
        String username,

        @Email(message = "Invalid email format")
        String email
) {
}
