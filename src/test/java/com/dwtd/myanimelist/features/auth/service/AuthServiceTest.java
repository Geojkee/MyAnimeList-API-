package com.dwtd.myanimelist.features.auth.service;

import com.dwtd.myanimelist.features.auth.dto.AuthResponse;
import com.dwtd.myanimelist.features.auth.dto.LoginRequest;
import com.dwtd.myanimelist.features.auth.dto.RegisterRequest;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

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

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);

        User user = User.builder()
                .id(1L)
                .username("TestUser")
                .role(Role.ROLE_USER)
                .build();

        when(userService.loadUserByUsername("TestUser")).thenReturn(user);

        String token = "jwt.token.here";
        when(jwtService.generateToken(user)).thenReturn(token);

        AuthResponse response = authService.signIn(request);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("TestUser");
        assertThat(response.token()).isEqualTo(token);
        assertThat(response.role()).isEqualTo(Role.ROLE_USER);

        verify(authenticationManager).authenticate(
                new UsernamePasswordAuthenticationToken("TestUser", "Password123")
        );
        verify(userService).loadUserByUsername("TestUser");
    }

    @Test
    void signIn_ShouldThrowException_WhenAuthenticationFails() {
        LoginRequest request = new LoginRequest("TestUser", "wrongPass");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new RuntimeException("Bad credentials"));

        assertThatThrownBy(() -> authService.signIn(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Bad credentials");

        verify(authenticationManager).authenticate(any());
        verifyNoInteractions(jwtService);
    }
}
