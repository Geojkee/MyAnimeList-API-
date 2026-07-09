package com.dwtd.myanimelist.features.auth.dto;

import com.dwtd.myanimelist.features.auth.enums.Role;
import lombok.Builder;

@Builder
public record RefreshTokenResponse(
        String accessToken,
        Long userId,
        String username,
        Role role
) {
}
