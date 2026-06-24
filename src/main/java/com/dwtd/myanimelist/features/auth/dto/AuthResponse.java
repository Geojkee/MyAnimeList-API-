package com.dwtd.myanimelist.features.auth.dto;

import com.dwtd.myanimelist.features.auth.enums.Role;
import lombok.Builder;

@Builder
public record AuthResponse(
        Long userId,
        String username,
        Role role,
        String token
) {
}