package com.dwtd.myanimelist.features.auth;

import com.dwtd.myanimelist.features.auth.dto.AuthResponse;
import com.dwtd.myanimelist.features.auth.dto.LoginRequest;
import com.dwtd.myanimelist.features.auth.dto.RegisterRequest;
import com.dwtd.myanimelist.features.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = "Authentification")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "User registration")
    @PostMapping("/sign-up")
    public AuthResponse signUp(@RequestBody @Valid RegisterRequest request) {
        return authService.signUp(request);
    }

    @Operation(summary = "User login")
    @PostMapping("/sign-in")
    public AuthResponse signIn(@RequestBody @Valid LoginRequest request) {
        return authService.signIn(request);
    }
}