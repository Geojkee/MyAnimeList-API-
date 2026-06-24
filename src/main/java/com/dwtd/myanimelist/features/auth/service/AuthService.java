package com.dwtd.myanimelist.features.auth.service;

import com.dwtd.myanimelist.features.auth.dto.AuthResponse;
import com.dwtd.myanimelist.features.auth.dto.LoginRequest;
import com.dwtd.myanimelist.features.auth.dto.RegisterRequest;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthResponse signUp(RegisterRequest request) {
        log.info("Registering new user with username: {}", request.username());

        var user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.ROLE_USER)
                .build();

        User saveduser = userService.create(user);

        var jwt = jwtService.generateToken(saveduser);

        log.info("User {} registered successfully with id: {}", saveduser.getUsername(), saveduser.getId());

        return AuthResponse.builder()
                .userId(saveduser.getId())
                .username(saveduser.getUsername())
                .token(jwt)
                .role(saveduser.getRole())
                .build();
    }

    public AuthResponse signIn(LoginRequest request) {
        log.info("Login attempt for username: {}", request.username());

        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.username(),
                request.password()
        ));

        var user = (User) userService
                .loadUserByUsername(request.username());

        var jwt = jwtService.generateToken(user);

        log.info("User {} logged in successfully", user.getUsername());

        return AuthResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .token(jwt)
                .build();
    }
}