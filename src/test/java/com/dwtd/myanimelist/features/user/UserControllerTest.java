package com.dwtd.myanimelist.features.user;

import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.helpers.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserControllerTest extends BaseIntegrationTest {

    private final String USERNAME = "TestUser";
    private final String EMAIL = "test@example.com";
    private final String PASSWORD = "Password123";

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        saveUser(USERNAME, EMAIL, PASSWORD, Role.ROLE_USER);
        userToken = loginAndGetToken(USERNAME, PASSWORD);
    }

    @Test
    void getMe_shouldReturnPrivateProfile_whenAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.email").value(EMAIL))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void getMe_shouldReturnUnauthorized_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void getPublicProfile_shouldReturnProfileWithoutEmail() throws Exception {
        mockMvc.perform(get("/api/v1/users/{username}", USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void getPublicProfile_shouldReturnNotFound_whenUserMissing() throws Exception {
        mockMvc.perform(get("/api/v1/users/{username}", "MissingUser"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
    }

    @Test
    void updateMe_shouldReturnUpdatedProfileAndToken() throws Exception {
        String body = """
                {
                    "username": "NewUser",
                    "email": "new@example.com"
                }
                """;

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(userToken))
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("NewUser"))
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void updateMe_shouldAllowUsingNewToken_whenUsernameChanged() throws Exception {
        String body = """
                {
                    "username": "NewUser"
                }
                """;

        String newToken = objectMapper.readTree(
                mockMvc.perform(patch("/api/v1/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .header("Authorization", bearer(userToken))
                                .content(body))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString()
        ).get("token").asText();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", bearer(newToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("NewUser"));
    }

    @Test
    void updateMe_shouldReturnConflict_whenUsernameTaken() throws Exception {
        saveUser("Taken", "taken@example.com", PASSWORD, Role.ROLE_USER);
        String body = """
                {
                    "username": "Taken"
                }
                """;

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(userToken))
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("USERNAME_ALREADY_EXISTS"));
    }

    @Test
    void updateMe_shouldReturnConflict_whenEmailTaken() throws Exception {
        saveUser("Other", "other@example.com", PASSWORD, Role.ROLE_USER);
        String body = """
                {
                    "email": "other@example.com"
                }
                """;

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(userToken))
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void updateMe_shouldReturnBadRequest_whenInvalidData() throws Exception {
        String body = """
                {
                    "username": "ab",
                    "email": "not-an-email"
                }
                """;

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(userToken))
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void updateMe_shouldReturnUnauthorized_whenNoToken() throws Exception {
        String body = """
                {
                    "email": "new@example.com"
                }
                """;

        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }
}
