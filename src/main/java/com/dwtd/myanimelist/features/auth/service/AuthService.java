package com.dwtd.myanimelist.features.auth.service;

import com.dwtd.myanimelist.exception.user.InvalidCredentialsException;
import com.dwtd.myanimelist.features.auth.dto.AuthResponse;
import com.dwtd.myanimelist.features.auth.dto.LoginRequest;
import com.dwtd.myanimelist.features.auth.dto.RefreshTokenResponse;
import com.dwtd.myanimelist.features.auth.dto.RegisterRequest;
import com.dwtd.myanimelist.features.auth.entity.RefreshToken;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.cookie.refresh-path}")
    private String refreshCookiePath;

    @Value("${app.cookie.max-age-seconds}")
    private long refreshTokenMaxAgeSeconds;

    @Value("${app.cookie.secure:false}")
    private boolean cookieSecure;


    public AuthResponse signUp(RegisterRequest request) {
        log.info("Registering new user with username: {}", request.username());

        var user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.ROLE_USER)
                .build();

        User saveduser = userService.create(user);

        String accessToken = jwtService.generateToken(saveduser);

        log.info("User {} registered successfully with id: {}", saveduser.getUsername(), saveduser.getId());

        return AuthResponse.builder()
                .userId(saveduser.getId())
                .username(saveduser.getUsername())
                .token(accessToken)
                .role(saveduser.getRole())
                .build();
    }

    public AuthResponse signIn(LoginRequest request, HttpServletResponse response) {
        log.info("Login attempt for username: {}", request.username());

        User user = userService.getByUsername(request.username());

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            log.warn("Login failed: invalid password for username={}", request.username());

            throw new InvalidCredentialsException(request.username());
        }
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        setRefreshTokenCookie(response, refreshToken.getToken());

        String accessToken = jwtService.generateToken(user);
        log.info("User {} logged in successfully", user.getUsername());

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .token(accessToken)
                .build();
    }

    public RefreshTokenResponse refresh(String refreshTokenValue, HttpServletResponse response) {
        RefreshToken refreshToken = refreshTokenService.validateRefreshToken(refreshTokenValue);
        User user = refreshToken.getUser();

        refreshTokenService.revokeToken(refreshTokenValue);

        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user);
        setRefreshTokenCookie(response, newRefreshToken.getToken());

        String newAccessToken = jwtService.generateToken(user);
        log.info("Refreshed tokens for user: {}", user.getUsername());

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .build();
    }

    public void logout(String refreshTokenValue, HttpServletResponse response){
        log.info("Logout with token: {}", refreshTokenValue);
        if (refreshTokenValue != null){
            log.info("Token revoked");
            refreshTokenService.revokeToken(refreshTokenValue);
        } else {
            log.warn("No refresh token in request");
        }

        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path(refreshCookiePath)
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", deleteCookie.toString());
        log.info("User logged out");
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String token){
        ResponseCookie deleteCookie = ResponseCookie.from("refreshToken", token)
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path(refreshCookiePath)
                .maxAge(Duration.ofSeconds(refreshTokenMaxAgeSeconds))
                .build();
        response.addHeader("Set-Cookie", deleteCookie.toString());
    }
}