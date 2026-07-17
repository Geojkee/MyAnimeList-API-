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
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }


    @Test
    void create_anime_shouldReturnCreated_whenAdmin() throws Exception {

        String animeJson = """
            {
                "titleRomaji": "Test Anime",
                "titleEnglish": "Test Anime English",
                "type": "TV",
                "episodeCount": 12,
                "status": "FINISHED",
                "synopsis": "Test synopsis"
            }
            """;

        mockMvc.perform(post("/anime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(animeJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.titleRomaji").value("Test Anime"))
                .andExpect(jsonPath("$.type").value("TV"))
                .andExpect(jsonPath("$.episodeCount").value(12))
                .andExpect(jsonPath("$.status").value("FINISHED"));
    }

    @Test
    void getAll_shouldReturnPageOfAnime_whenPublic() throws Exception {

        Anime anime1 = Anime.builder()
                .titleRomaji("Naruto")
                .type(AnimeType.TV)
                .episodeCount(220)
                .status(AnimeStatus.FINISHED)
                .build();
        animeRepository.save(anime1);

        Anime anime2 = Anime.builder()
                .titleRomaji("Attack on Titan")
                .type(AnimeType.TV)
                .episodeCount(24)
                .status(AnimeStatus.FINISHED)
                .build();
        animeRepository.save(anime2);

        mockMvc.perform(get("/anime")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getById_shouldReturnAnime_whenExists() throws Exception {
        Anime anime = Anime.builder()
                .titleRomaji("Attack on Titan")
                .type(AnimeType.TV)
                .episodeCount(87)
                .status(AnimeStatus.FINISHED)
                .build();
        Anime saved = animeRepository.save(anime);

        mockMvc.perform(get("/anime/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.titleRomaji").value("Attack on Titan"));
    }

    @Test
    void getById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/anime/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ANIME_NOT_FOUND"));
    }

    @Test
    void create_shouldReturnForbidden_whenUser() throws Exception {
        String requestJson = """
            {
                "titleRomaji": "Test Anime",
                "titleEnglish": "Test Anime English",
                "type": "TV",
                "episodeCount": 12,
                "status": "FINISHED",
                "synopsis": "Test synopsis"
            }
            """;

        mockMvc.perform(post("/anime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(requestJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_shouldReturnUnauthorized_whenNoToken() throws Exception {
        String requestJson = """
            {
                "titleRomaji": "Test Anime",
                "titleEnglish": "Test Anime English",
                "type": "TV",
                "episodeCount": 12,
                "status": "FINISHED",
                "synopsis": "Test synopsis"
            }
            """;

        mockMvc.perform(post("/anime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_shouldReturnBadRequest_whenInvalidData() throws Exception {
        String invalidJson = """
            {
                "titleRomaji": "",
                "type": "TV",
                "episodeCount": -5,
                "status": "FINISHED"
            }
            """;

        mockMvc.perform(post("/anime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + adminToken)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }
}
