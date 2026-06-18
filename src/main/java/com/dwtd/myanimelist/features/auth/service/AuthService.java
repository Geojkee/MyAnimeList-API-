package com.dwtd.myanimelist.features.auth.service;

import com.dwtd.myanimelist.features.auth.DTO.AuthResponse;
import com.dwtd.myanimelist.features.auth.DTO.LoginRequest;
import com.dwtd.myanimelist.features.auth.DTO.RegisterRequest;
import com.dwtd.myanimelist.features.auth.Enum.Role;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthResponse signUp(RegisterRequest request){
        var user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(Role.ROLE_USER)
                .build();

        userService.create(user);

        var jwt = jwtService.generateToken(user);

        return new AuthResponse(user.getUsername(), jwt);
    }

    public AuthResponse signIn(LoginRequest request){
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                request.username(),
                request.password()
        ));

        var user = userService
                .userDetailsService()
                .loadUserByUsername(request.username());

        var jwt = jwtService.generateToken(user);
        return new AuthResponse(user.getUsername(), jwt);
    }
}
