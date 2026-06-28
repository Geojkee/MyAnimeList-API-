package com.dwtd.myanimelist.features.auth;

import com.dwtd.myanimelist.features.auth.dto.LoginRequest;
import com.dwtd.myanimelist.features.auth.dto.RegisterRequest;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    private final String USERNAME = "TestUser";
    private final String EMAIL = "test@example.com";
    private final String PASSWORD = "Password123";

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private void createUser() {
        User user = User.builder()
                .username(USERNAME)
                .email(EMAIL)
                .password(passwordEncoder.encode(PASSWORD))
                .build();

        userRepository.save(user);
    }

    @Test
    void signUp_ShouldReturnToken_WhenValidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(
                USERNAME,
                EMAIL,
                PASSWORD
        );

        mockMvc.perform(post("/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.username").value("TestUser"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void signUp_ShouldReturnConflict_WhenUsernameAlreadyExists() throws Exception {
        createUser();

        RegisterRequest request = new RegisterRequest(
                USERNAME,
                "new@example.com",
                PASSWORD
        );

        mockMvc.perform(post("/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USERNAME_ALREADY_EXISTS"));
    }

    @Test
    void signUp_ShouldReturnConflict_WhenEmailAlreadyExists() throws Exception {
        createUser();

        RegisterRequest request = new RegisterRequest(
                "newUser",
                EMAIL,
                PASSWORD
        );

        mockMvc.perform(post("/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void signIn_ShouldReturnToken_WhenValidCredentials() throws Exception {
        createUser();

        LoginRequest loginRequest = new LoginRequest(
                USERNAME,
                PASSWORD
        );

        mockMvc.perform(post("/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void signIn_ShouldReturnUnauthorized_WhenInvalidPassword() throws Exception {
        createUser();

        LoginRequest loginRequest = new LoginRequest(
                USERNAME,
                "wrongPassword"
        );

        mockMvc.perform(post("/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void signIn_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        LoginRequest loginRequest = new LoginRequest(
                USERNAME,
                PASSWORD
        );

        mockMvc.perform(post("/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
    }
}
