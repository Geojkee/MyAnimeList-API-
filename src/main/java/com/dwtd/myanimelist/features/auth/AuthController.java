package com.dwtd.myanimelist.features.auth;

import com.dwtd.myanimelist.features.auth.dto.AuthResponse;
import com.dwtd.myanimelist.features.auth.dto.LoginRequest;
import com.dwtd.myanimelist.features.auth.dto.RefreshTokenResponse;
import com.dwtd.myanimelist.features.auth.dto.RegisterRequest;
import com.dwtd.myanimelist.features.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentification")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "User registration")
    @PostMapping("/sign-up")
    public ResponseEntity<AuthResponse> signUp(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(authService.signUp(request));
    }

    @Operation(summary = "User login")
    @PostMapping("/sign-in")
    public ResponseEntity<AuthResponse> signIn(
            @RequestBody @Valid LoginRequest request,
            HttpServletResponse response
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(authService.signIn(request, response));
    }

    @Operation(summary = "Refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = extractRefreshToken(request);
        return ResponseEntity.status(HttpStatus.OK).body(authService.refresh(refreshToken, response));
    }

    @Operation(summary = "User logout")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = extractRefreshToken(request);
        authService.logout(refreshToken, response);

        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())){
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}