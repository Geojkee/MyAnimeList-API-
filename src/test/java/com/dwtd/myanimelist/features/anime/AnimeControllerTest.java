package com.dwtd.myanimelist.features.anime;

import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.anime.enums.AnimeStatus;
import com.dwtd.myanimelist.features.anime.enums.AnimeType;
import com.dwtd.myanimelist.features.anime.repository.AnimeRepository;
import com.dwtd.myanimelist.features.auth.dto.LoginRequest;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.auth.repository.RefreshTokenRepository;
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
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AnimeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnimeRepository animeRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String ADMIN_USERNAME = "Admin";
    private final String ADMIN_EMAIL = "admin@example.com";
    private final String ADMIN_PASSWORD = "AdminPassword";

    private final String USER_USERNAME = "User";
    private final String USER_EMAIL = "user@example.com";
    private final String USER_PASSWORD = "UserPassword";

    private String adminToken;
    private String userToken;
    private Anime testAnime1;
    private Anime testAnime2;
    private Anime testAnime3;
    private Anime testAnime4;

    @BeforeEach
    void setUp() throws Exception {
        refreshTokenRepository.deleteAll();
        animeRepository.deleteAll();
        userRepository.deleteAll();

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

        testAnime1 = Anime.builder()
                .titleRomaji("Naruto")
                .type(AnimeType.TV)
                .episodeCount(220)
                .status(AnimeStatus.FINISHED)
                .build();
        animeRepository.save(testAnime1);

        testAnime2 = Anime.builder()
                .titleRomaji("Attack on Titan")
                .type(AnimeType.TV)
                .episodeCount(87)
                .status(AnimeStatus.FINISHED)
                .build();
        animeRepository.save(testAnime2);

        testAnime3 = Anime.builder()
                .titleRomaji("Wasted Chef")
                .type(AnimeType.MOVIE)
                .episodeCount(1)
                .status(AnimeStatus.ANNOUNCED)
                .build();
        animeRepository.save(testAnime3);

        testAnime4 = Anime.builder()
                .titleRomaji("Mushoku Tensei")
                .type(AnimeType.TV)
                .episodeCount(3)
                .status(AnimeStatus.ONGOING)
                .build();
        animeRepository.save(testAnime4);
    }

    private String validAnimeJson() {
        return """
                {
                    "titleRomaji": "Test Anime",
                    "titleEnglish": "Test Anime English",
                    "type": "TV",
                    "episodeCount": 12,
                    "status": "FINISHED",
                    "synopsis": "Test synopsis"
                }
                """;
    }

    private String invalidAnimeJson() {
        return """
                {
                    "titleRomaji": "",
                    "type": "TV",
                    "episodeCount": -5,
                    "status": "FINISHED"
                }
                """;
    }

    private String updateAnimeJson() {
        return """
                {
                    "titleRomaji": "Naruto (update)",
                    "titleEnglish": "Naruto English (update)",
                    "type": "TV",
                    "episodeCount": 221,
                    "status": "FINISHED",
                    "synopsis": "Naruto synopsis (update)"
                }
                """;
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asString();
    }

    @Test
    void create_anime_shouldReturnCreated_whenAdmin() throws Exception {

        mockMvc.perform(post("/anime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(validAnimeJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.titleRomaji").value("Test Anime"))
                .andExpect(jsonPath("$.type").value("TV"))
                .andExpect(jsonPath("$.episodeCount").value(12))
                .andExpect(jsonPath("$.status").value("FINISHED"));
    }

    @Test
    void create_shouldReturnForbidden_whenUser() throws Exception {
        mockMvc.perform(post("/anime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(validAnimeJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_shouldReturnUnauthorized_whenNoToken() throws Exception {
        mockMvc.perform(post("/anime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAnimeJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_shouldReturnBadRequest_whenInvalidData() throws Exception {
        mockMvc.perform(post("/anime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(invalidAnimeJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void getAll_shouldReturnPageOfAnime_whenPublic() throws Exception {

        mockMvc.perform(get("/anime")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    void getById_shouldReturnAnime_whenExists() throws Exception {
        mockMvc.perform(get("/anime/{id}", testAnime1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testAnime1.getId()))
                .andExpect(jsonPath("$.titleRomaji").value("Naruto"));
    }

    @Test
    void getById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/anime/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ANIME_NOT_FOUND"));
    }

    @Test
    void update_anime_shouldReturnOk_whenAdmin() throws Exception {
        mockMvc.perform(put("/anime/{id}", testAnime1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(updateAnimeJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testAnime1.getId()))
                .andExpect(jsonPath("$.titleRomaji").value("Naruto (update)"))
                .andExpect(jsonPath("$.titleEnglish").value("Naruto English (update)"))
                .andExpect(jsonPath("$.episodeCount").value(221))
                .andExpect(jsonPath("$.status").value(AnimeStatus.FINISHED.name()))
                .andExpect(jsonPath("$.synopsis").value("Naruto synopsis (update)"));
    }

    @Test
    void update_anime_shouldReturnForbidden_whenUser() throws Exception {
        mockMvc.perform(put("/anime/{id}", testAnime1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(updateAnimeJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void update_anime_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(put("/anime/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(updateAnimeJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ANIME_NOT_FOUND"));
    }

    @Test
    void delete_shouldReturnNoContent_whenAdmin() throws Exception {
        mockMvc.perform(delete("/anime/{id}", testAnime1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturnForbidden_whenUser() throws Exception {
        mockMvc.perform(delete("/anime/{id}", testAnime1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void delete_shouldReturnNotFound_whenNotExists() throws Exception {
        mockMvc.perform(delete("/anime/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ANIME_NOT_FOUND"));
    }

    @Test
    void delete_shouldReturnUnauthorized_whenNoToken() throws Exception {
        mockMvc.perform(delete("/anime/{id}", testAnime1.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAll_shouldFilterBySearch() throws Exception {
        mockMvc.perform(get("/anime")
                        .param("search", "Titan")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titleRomaji").value("Attack on Titan"));
    }

    @Test
    void getAll_shouldFilterByType() throws Exception {
        mockMvc.perform(get("/anime")
                        .param("type", "MOVIE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titleRomaji").value("Wasted Chef"));
    }

    @Test
    void getAll_shouldFilterByStatus() throws Exception {
        mockMvc.perform(get("/anime")
                        .param("status", "FINISHED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].titleRomaji").value("Naruto"))
                .andExpect(jsonPath("$.content[1].titleRomaji").value("Attack on Titan"));
    }

    @Test
    void getAll_shouldFilterByCombination() throws Exception {
        mockMvc.perform(get("/anime")
                        .param("type", "TV")
                        .param("status", "ONGOING")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titleRomaji").value("Mushoku Tensei"));
    }
}