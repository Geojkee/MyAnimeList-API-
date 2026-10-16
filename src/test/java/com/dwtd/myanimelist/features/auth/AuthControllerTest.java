package com.dwtd.myanimelist.features.auth;

import com.dwtd.myanimelist.features.auth.dto.LoginRequest;
import com.dwtd.myanimelist.features.auth.dto.RegisterRequest;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.helpers.BaseIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthControllerTest extends BaseIntegrationTest {

    private final String USERNAME = "TestUser";
    private final String EMAIL = "test@example.com";
    private final String PASSWORD = "Password123";

    private void createUser() {
        saveUser(USERNAME, EMAIL, PASSWORD, Role.ROLE_USER);
    }

    @Test
    void signUp_ShouldReturnToken_WhenValidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest(USERNAME, EMAIL, PASSWORD);

        mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.username").value("TestUser"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void signUp_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        RegisterRequest request = new RegisterRequest("", "invalid-email", "short");

        mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void signUp_ShouldReturnConflict_WhenUsernameAlreadyExists() throws Exception {
        createUser();

        RegisterRequest request = new RegisterRequest(USERNAME, "new@example.com", PASSWORD);

        mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USERNAME_ALREADY_EXISTS"));
    }

    @Test
    void signUp_ShouldReturnConflict_WhenEmailAlreadyExists() throws Exception {
        createUser();

        RegisterRequest request = new RegisterRequest("newUser", EMAIL, PASSWORD);

        mockMvc.perform(post("/api/v1/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void signIn_ShouldReturnToken_WhenValidCredentials() throws Exception {
        createUser();

        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(USERNAME, PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void signIn_ShouldReturnUnauthorized_WhenInvalidPassword() throws Exception {
        createUser();

        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(USERNAME, "wrongPassword"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void signIn_ShouldReturnNotFound_WhenUserDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(USERNAME, PASSWORD))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
    }

    @Test
    void refresh_ShouldReturnNewAccessToken_WhenValidRefreshToken() throws Exception {
        createUser();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(USERNAME, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = loginResult.getResponse().getCookie("refreshToken").getValue();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.username").value(USERNAME));
    }

    @Test
    void refresh_ShouldReturnNotFound_WhenNoRefreshCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("TOKEN_NOT_FOUND"));
    }

    @Test
    void refresh_ShouldIgnoreUnrelatedCookies() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("other", "value")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("TOKEN_NOT_FOUND"));
    }

    @Test
    void logout_ShouldRevokeRefreshTokenAndDeleteCookie() throws Exception {
        createUser();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(USERNAME, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("token").asText();
        String refreshToken = loginResult.getResponse().getCookie("refreshToken").getValue();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", bearer(accessToken))
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logout_ShouldSucceed_WhenRefreshCookieMissing() throws Exception {
        createUser();
        String accessToken = loginAndGetToken(USERNAME, PASSWORD);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void protectedEndpoint_ShouldReturnUnauthorized_WhenJwtMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }
}
