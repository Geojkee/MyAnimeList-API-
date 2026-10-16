package com.dwtd.myanimelist.features.user.dto;

import com.dwtd.myanimelist.features.auth.enums.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserProfileResponse(
        Long id,
        String username,
        String email,
        Role role,
        Instant createdAt,
        String token
) {
}
