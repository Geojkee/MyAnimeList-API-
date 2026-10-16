package com.dwtd.myanimelist.features.genre;

import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.helpers.BaseIntegrationTest;
import com.dwtd.myanimelist.helpers.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class GenreControllerTest extends BaseIntegrationTest {

    private final String ADMIN_USERNAME = "Admin";
    private final String ADMIN_PASSWORD = "AdminPassword";
    private final String USER_USERNAME = "User";
    private final String USER_PASSWORD = "UserPassword";

    private Long romanceGenreId;
    private Long adventureGenreId;
    private Long comedyGenreId;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        saveUser(ADMIN_USERNAME, "admin@example.com", ADMIN_PASSWORD, Role.ROLE_ADMIN);
        saveUser(USER_USERNAME, "user@example.com", USER_PASSWORD, Role.ROLE_USER);

        adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        userToken = loginAndGetToken(USER_USERNAME, USER_PASSWORD);

        romanceGenreId = genreRepository.save(TestDataFactory.genre("Romance")).getId();
        adventureGenreId = genreRepository.save(TestDataFactory.genre("Adventure")).getId();
        comedyGenreId = genreRepository.save(TestDataFactory.genre("Comedy")).getId();
    }

    private String validGenreJson() {
        return """
                {
                    "name": "Test Genre"
                }
                """;
    }

    private String invalidGenreJson() {
        return """
                {
                    "name": ""
                }
                """;
    }

    private String updateGenreJson() {
        return """
                {
                    "name": "Update Genre"
                }
                """;
    }

    @Test
    void create_genre_shouldReturnCreated_whenAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/genre")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content(validGenreJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Test Genre"));
    }

    @Test
    void create_shouldReturnUnauthorized_whenNoToken() throws Exception {
        mockMvc.perform(post("/api/v1/genre")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validGenreJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void create_shouldReturnForbidden_whenUser() throws Exception {
        mockMvc.perform(post("/api/v1/genre")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(userToken))
                        .content(validGenreJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void create_shouldReturnBadRequest_whenInvalidData() throws Exception {
        mockMvc.perform(post("/api/v1/genre")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content(invalidGenreJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void create_shouldReturnConflict_whenDuplicateName() throws Exception {
        mockMvc.perform(post("/api/v1/genre")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content(validGenreJson()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/genre")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content(validGenreJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("GENRE_ALREADY_EXISTS"));
    }

    @Test
    void update_shouldReturnOk_whenAdmin() throws Exception {
        mockMvc.perform(put("/api/v1/genre/{id}", romanceGenreId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content(updateGenreJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Update Genre"));
    }

    @Test
    void update_shouldReturnUnauthorized_whenNoToken() throws Exception {
        mockMvc.perform(put("/api/v1/genre/{id}", romanceGenreId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateGenreJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void update_shouldReturnForbidden_whenUser() throws Exception {
        mockMvc.perform(put("/api/v1/genre/{id}", romanceGenreId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(userToken))
                        .content(updateGenreJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void update_shouldReturnBadRequest_whenInvalidData() throws Exception {
        mockMvc.perform(put("/api/v1/genre/{id}", romanceGenreId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content(invalidGenreJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void update_shouldReturnConflict_whenDuplicateName() throws Exception {
        String updateJson = """
            {
                "name": "Romance"
            }
            """;

        mockMvc.perform(put("/api/v1/genre/{id}", adventureGenreId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content(updateJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("GENRE_ALREADY_EXISTS"));
    }

    @Test
    void update_shouldReturnNotFound_whenGenreNotExists() throws Exception {
        mockMvc.perform(put("/api/v1/genre/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content(validGenreJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("GENRE_NOT_FOUND"));
    }

    @Test
    void delete_shouldReturnNoContent_whenAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/genre/{id}", romanceGenreId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturnUnauthorized_whenNoToken() throws Exception {
        mockMvc.perform(delete("/api/v1/genre/{id}", romanceGenreId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void delete_shouldReturnForbidden_whenUser() throws Exception {
        mockMvc.perform(delete("/api/v1/genre/{id}", romanceGenreId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void delete_shouldReturnNotFound_whenGenreNotExists() throws Exception {
        mockMvc.perform(delete("/api/v1/genre/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("GENRE_NOT_FOUND"));
    }

    @Test
    void getAll_shouldReturnListOfGenres_whenPublic() throws Exception {
        mockMvc.perform(get("/api/v1/genre")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Adventure"))
                .andExpect(jsonPath("$[1].name").value("Comedy"))
                .andExpect(jsonPath("$[2].name").value("Romance"));
    }

    @Test
    void getById_shouldReturnGenre_whenExists() throws Exception {
        mockMvc.perform(get("/api/v1/genre/{id}", comedyGenreId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Comedy"));
    }

    @Test
    void getById_shouldReturnNotFound_whenNotExists() throws Exception {
        mockMvc.perform(get("/api/v1/genre/999999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("GENRE_NOT_FOUND"));
    }
}
