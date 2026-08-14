package com.dwtd.myanimelist.features.genre;

import com.dwtd.myanimelist.features.auth.dto.LoginRequest;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.auth.repository.RefreshTokenRepository;
import com.dwtd.myanimelist.features.auth.repository.UserRepository;
import com.dwtd.myanimelist.features.genre.entity.Genre;
import com.dwtd.myanimelist.features.genre.repository.GenreRepository;
import com.dwtd.myanimelist.features.tracking.repository.UserAnimeListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GenreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserAnimeListRepository userAnimeListRepository;

    private final String ADMIN_USERNAME = "Admin";
    private final String ADMIN_EMAIL = "admin@example.com";
    private final String ADMIN_PASSWORD = "AdminPassword";

    private final String USER_USERNAME = "User";
    private final String USER_EMAIL = "user@example.com";
    private final String USER_PASSWORD = "UserPassword";

    @Autowired
    private GenreRepository genreRepository;
    private Long romanceGenreId;
    private Long adventureGenreId;
    private Long comedyGenreId;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        refreshTokenRepository.deleteAll();
        userAnimeListRepository.deleteAll();
        userRepository.deleteAll();
        genreRepository.deleteAll();

        User admin = User.builder()
                .username(ADMIN_USERNAME)
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .role(Role.ROLE_ADMIN)
                .build();
        userRepository.save(admin);

        User user = User.builder()
                .username(USER_USERNAME)
                .email(USER_EMAIL)
                .password(passwordEncoder.encode(USER_PASSWORD))
                .role(Role.ROLE_USER)
                .build();
        userRepository.save(user);

        adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        userToken = loginAndGetToken(USER_USERNAME, USER_PASSWORD);


        Genre romance = genreRepository.save(Genre.builder().name("Romance").build());
        romanceGenreId = romance.getId();
        Genre adventure = genreRepository.save(Genre.builder().name("Adventure").build());
        adventureGenreId = adventure.getId();
        Genre comedy = genreRepository.save(Genre.builder().name("Comedy").build());
        comedyGenreId = comedy.getId();
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asString();
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
                        .header("Authorization", "Bearer " + adminToken)
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
                        .header("Authorization", "Bearer " + userToken)
                        .content(validGenreJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void create_shouldReturnBadRequest_whenInvalidData() throws Exception {
        mockMvc.perform(post("/api/v1/genre")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(invalidGenreJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void create_shouldReturnConflict_whenDuplicateName() throws Exception {
        mockMvc.perform(post("/api/v1/genre")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(validGenreJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Test Genre"));

        mockMvc.perform(post("/api/v1/genre")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(validGenreJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("GENRE_ALREADY_EXISTS"));
    }

    @Test
    void update_shouldReturnOk_whenAdmin() throws Exception {
        mockMvc.perform(put("/api/v1/genre/{id}", romanceGenreId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
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
                        .header("Authorization", "Bearer " + userToken)
                        .content(updateGenreJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void update_shouldReturnBadRequest_whenInvalidData() throws Exception {
        mockMvc.perform(put("/api/v1/genre/{id}", romanceGenreId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
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
                        .header("Authorization", "Bearer " + adminToken)
                        .content(updateJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("GENRE_ALREADY_EXISTS"));
    }

    @Test
    void update_shouldReturnNotFound_whenGenreNotExists() throws Exception {
        mockMvc.perform(put("/api/v1/genre/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(validGenreJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("GENRE_NOT_FOUND"));
    }

    @Test
    void delete_shouldReturnNoContent_whenAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/genre/{id}", romanceGenreId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken))
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
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void delete_shouldReturnNotFound_whenGenreNotExists() throws Exception {
        mockMvc.perform(delete("/api/v1/genre/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken))
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
        mockMvc.perform(get("/api/v1/genre/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("GENRE_NOT_FOUND"));
    }
}
