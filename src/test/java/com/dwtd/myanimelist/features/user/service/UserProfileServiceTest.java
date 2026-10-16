package com.dwtd.myanimelist.features.user.service;

import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.auth.service.UserService;
import com.dwtd.myanimelist.features.user.dto.UpdateUserProfileRequest;
import com.dwtd.myanimelist.features.user.dto.UserProfileResponse;
import com.dwtd.myanimelist.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserProfileService userProfileService;

    private final User user = User.builder()
            .id(1L)
            .username("Alice")
            .email("alice@example.com")
            .role(Role.ROLE_USER)
            .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
            .build();

    @Test
    void getCurrentProfile_shouldIncludeEmailAndOmitToken() {
        when(userService.getCurrentUser()).thenReturn(user);

        UserProfileResponse response = userProfileService.getCurrentProfile();

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("Alice");
        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.token()).isNull();
    }

    @Test
    void getPublicProfile_shouldOmitEmailAndToken() {
        when(userService.getByUsername("Alice")).thenReturn(user);

        UserProfileResponse response = userProfileService.getPublicProfile("Alice");

        assertThat(response.username()).isEqualTo("Alice");
        assertThat(response.email()).isNull();
        assertThat(response.token()).isNull();
    }

    @Test
    void updateCurrentProfile_shouldReturnNewToken() {
        UpdateUserProfileRequest request = new UpdateUserProfileRequest("AliceNew", "new@example.com");
        User updated = User.builder()
                .id(1L)
                .username("AliceNew")
                .email("new@example.com")
                .role(Role.ROLE_USER)
                .createdAt(user.getCreatedAt())
                .build();
        when(userService.updateCurrentUser("AliceNew", "new@example.com")).thenReturn(updated);
        when(jwtService.generateToken(updated)).thenReturn("new.jwt");

        UserProfileResponse response = userProfileService.updateCurrentProfile(request);

        assertThat(response.username()).isEqualTo("AliceNew");
        assertThat(response.email()).isEqualTo("new@example.com");
        assertThat(response.token()).isEqualTo("new.jwt");
        verify(jwtService).generateToken(updated);
    }
}
