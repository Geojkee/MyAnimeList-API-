package com.dwtd.myanimelist.features.user.service;

import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.service.UserService;
import com.dwtd.myanimelist.features.user.dto.UpdateUserProfileRequest;
import com.dwtd.myanimelist.features.user.dto.UserProfileResponse;
import com.dwtd.myanimelist.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserService userService;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentProfile() {
        return toPrivateResponse(userService.getCurrentUser(), null);
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getPublicProfile(String username) {
        User user = userService.getByUsername(username);
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public UserProfileResponse updateCurrentProfile(UpdateUserProfileRequest request) {
        User updated = userService.updateCurrentUser(request.username(), request.email());
        String token = jwtService.generateToken(updated);
        log.info("Issued new access token after profile update for user id={}", updated.getId());
        return toPrivateResponse(updated, token);
    }

    private UserProfileResponse toPrivateResponse(User user, String token) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .token(token)
                .build();
    }
}
