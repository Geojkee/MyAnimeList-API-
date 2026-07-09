package com.dwtd.myanimelist.features.auth.service;

import com.dwtd.myanimelist.exception.user.InvalidCredentialsException;
import com.dwtd.myanimelist.features.auth.dto.AuthResponse;
import com.dwtd.myanimelist.features.auth.dto.LoginRequest;
import com.dwtd.myanimelist.features.auth.dto.RefreshTokenResponse;
import com.dwtd.myanimelist.features.auth.dto.RegisterRequest;
import com.dwtd.myanimelist.features.auth.entity.RefreshToken;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Test
    void signUp_ShouldRegisterUserAndReturnAuthResponse() {
        RegisterRequest request = new RegisterRequest(
                "TestUser",
                "test@example.com",
                "Password123"
        );

        String encodedPass = "encodePass";
        when(passwordEncoder.encode("Password123")).thenReturn(encodedPass);

        User savedUser = User.builder()
                .id(1L)
                .username("TestUser")
                .email("test@example.com")
                .password(encodedPass)
                .build();

        when(userService.create(any(User.class))).thenReturn(savedUser);

        String token = "jwt.token.here";
        when(jwtService.generateToken(any(User.class))).thenReturn(token);

        AuthResponse response = authService.signUp(request);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("TestUser");
        assertThat(response.token()).isEqualTo(token);
        assertThat(response.role()).isEqualTo(Role.ROLE_USER);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).create(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertThat(capturedUser.getUsername()).isEqualTo("TestUser");
        assertThat(capturedUser.getPassword()).isEqualTo(encodedPass);
        assertThat(capturedUser.getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    void signUp_ShouldThrowException_WhenUserAlreadyExists() {
        RegisterRequest request = new RegisterRequest(
                "TestUser",
                "test@example.com",
                "Password123"
        );

        when(passwordEncoder.encode("Password123")).thenReturn("encoded");
        when(userService.create(any(User.class)))
                .thenThrow(new RuntimeException("User with username TestUser already exists"));

        assertThatThrownBy(() -> authService.signUp(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already exists");

        verify(userService, times(1)).create(any(User.class));
        verifyNoInteractions(jwtService);
    }

    @Test
    void signIn_ShouldAuthenticateAndReturnAuthResponse() {
        LoginRequest request = new LoginRequest("TestUser", "Password123");

        User user = User.builder()
                .id(1L)
                .password("encodedPassword")
                .username("TestUser")
                .role(Role.ROLE_USER)
                .build();

        when(userService.getByUsername("TestUser")).thenReturn(user);
        when(passwordEncoder.matches("Password123", "encodedPassword")).thenReturn(true);

        String token = "jwt.token.here";
        when(jwtService.generateToken(user)).thenReturn(token);

        RefreshToken refreshToken = RefreshToken.builder()
                .token("refresh-token-uuid")
                .build();
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);

        AuthResponse response = authService.signIn(request, mockResponse);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("TestUser");
        assertThat(response.token()).isEqualTo(token);
        assertThat(response.role()).isEqualTo(Role.ROLE_USER);

        verify(userService).getByUsername("TestUser");
        verify(passwordEncoder).matches("Password123", "encodedPassword");
        verify(jwtService).generateToken(user);
        verify(refreshTokenService).createRefreshToken(user);
        verify(mockResponse).addHeader(eq("Set-Cookie"), anyString());
    }

    @Test
    void signIn_ShouldThrowException_WhenAuthenticationFails() {
        LoginRequest request = new LoginRequest("TestUser", "WrongPassword");

        User user = User.builder()
                .username("TestUser")
                .password("encodedPassword")
                .build();

        when(userService.getByUsername("TestUser")).thenReturn(user);
        when(passwordEncoder.matches("WrongPassword", "encodedPassword")).thenReturn(false);


        assertThatThrownBy(() -> authService.signIn(request, mockResponse))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("invalid credentials");

        verify(userService).getByUsername("TestUser");
        verify(passwordEncoder).matches("WrongPassword", "encodedPassword");
        verifyNoInteractions(jwtService);
    }

    @Test
    void refresh_ShouldReturnNewAccessToken_WhenValidRefreshToken() {
        String refreshTokenValue = "validRefreshToken";

        RefreshToken refreshToken = mock(RefreshToken.class);

        User user = User.builder().id(1L).username("TestUser").role(Role.ROLE_USER).build();

        when(refreshTokenService.validateRefreshToken(refreshTokenValue)).thenReturn(refreshToken);
        when(refreshToken.getUser()).thenReturn(user);

        doNothing().when(refreshTokenService).revokeToken(refreshTokenValue);

        RefreshToken newRefreshToken = mock(RefreshToken.class);

        when(newRefreshToken.getToken()).thenReturn("newRefreshToken");
        when(refreshTokenService.createRefreshToken(user)).thenReturn(newRefreshToken);
        when(jwtService.generateToken(user)).thenReturn("newAccessToken");

        HttpServletResponse response = mock(HttpServletResponse.class);
        RefreshTokenResponse responseDto = authService.refresh(refreshTokenValue, response);

        assertThat(responseDto.accessToken()).isEqualTo("newAccessToken");
        assertThat(responseDto.userId()).isEqualTo(1L);
    }

    @Test
    void logout_ShouldRevokeTokenAndDeleteCookie() {
        String refreshTokenValue = "validRefreshToken";

        HttpServletResponse response = mock(HttpServletResponse.class);

        doNothing().when(refreshTokenService).revokeToken(refreshTokenValue);

        authService.logout(refreshTokenValue, response);

        verify(refreshTokenService).revokeToken(refreshTokenValue);
        verify(response).addHeader(eq("Set-Cookie"), anyString());
    }
}
