package com.dwtd.myanimelist.features.tracking;

import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.anime.enums.AnimeStatus;
import com.dwtd.myanimelist.features.anime.enums.AnimeType;
import com.dwtd.myanimelist.features.anime.repository.AnimeRepository;
import com.dwtd.myanimelist.features.auth.dto.LoginRequest;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.auth.repository.RefreshTokenRepository;
import com.dwtd.myanimelist.features.auth.repository.UserRepository;
import com.dwtd.myanimelist.features.tracking.dto.UserAnimeListRequest;
import com.dwtd.myanimelist.features.tracking.entity.UserAnimeList;
import com.dwtd.myanimelist.features.tracking.enums.UserAnimeStatus;
import com.dwtd.myanimelist.features.tracking.repository.UserAnimeListRepository;
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
public class UserAnimeListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserAnimeListRepository userAnimeListRepository;

    @Autowired
    private AnimeRepository animeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String USERNAME = "TestUser";
    private final String PASSWORD = "Password123";
    private final String EMAIL = "test@example.com";

    private String userToken;
    private Anime testAnime;

    @BeforeEach
    void setUp() throws Exception {
        refreshTokenRepository.deleteAll();
        userAnimeListRepository.deleteAll();
        animeRepository.deleteAll();
        userRepository.deleteAll();

        User user = User.builder()
                .username(USERNAME)
                .email(EMAIL)
                .password(passwordEncoder.encode(PASSWORD))
                .role(Role.ROLE_USER)
                .build();
        userRepository.save(user);

        userToken = loginAndGetToken(USERNAME, PASSWORD);

        testAnime = Anime.builder()
                .titleRomaji("Naruto")
                .titleEnglish("Naruto")
                .type(AnimeType.TV)
                .episodeCount(220)
                .status(AnimeStatus.FINISHED)
                .build();
        animeRepository.save(testAnime);
    }

    private String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }

    @Test
    void addOrUpdate_shouldReturnOk_whenAddingNewEntry() throws Exception {
        UserAnimeListRequest request = new UserAnimeListRequest(
                testAnime.getId(),
                UserAnimeStatus.WATCHING,
                8,
                5
        );

        mockMvc.perform(post("/api/v1/users/me/animelist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.animeId").value(testAnime.getId()))
                .andExpect(jsonPath("$.status").value("WATCHING"))
                .andExpect(jsonPath("$.score").value(8))
                .andExpect(jsonPath("$.watchedEpisodes").value(5))
                .andExpect(jsonPath("$.totalEpisodes").value(220));
    }

    @Test
    void addOrUpdate_shouldReturnOk_whenUpdatingExistingEntry() throws Exception {
        User user = userRepository.findByUsername(USERNAME).get();
        UserAnimeList initial = UserAnimeList.builder()
                .user(user)
                .anime(testAnime)
                .status(UserAnimeStatus.WATCHING)
                .score(8)
                .watchedEpisodes(5)
                .build();
        userAnimeListRepository.save(initial);

        UserAnimeListRequest updateRequest = new UserAnimeListRequest(
                testAnime.getId(),
                UserAnimeStatus.COMPLETED,
                10,
                220
        );

        mockMvc.perform(post("/api/v1/users/me/animelist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.score").value(10))
                .andExpect(jsonPath("$.watchedEpisodes").value(220));
    }

    @Test
    void addOrUpdate_shouldReturnBadRequest_whenWatchedEpisodesExceedsTotal() throws Exception {
        UserAnimeListRequest request = new UserAnimeListRequest(
                testAnime.getId(),
                UserAnimeStatus.WATCHING,
                8,
                300
        );

        mockMvc.perform(post("/api/v1/users/me/animelist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_USER_ANIME_DATA"));
    }

    @Test
    void addOrUpdate_shouldReturnNotFound_whenAnimeNotExists() throws Exception {
        UserAnimeListRequest request = new UserAnimeListRequest(
                999L,
                UserAnimeStatus.WATCHING,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/users/me/animelist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ANIME_NOT_FOUND"));
    }

    @Test
    void addOrUpdate_shouldReturnUnauthorized_whenNoToken() throws Exception {
        UserAnimeListRequest request = new UserAnimeListRequest(
                testAnime.getId(),
                UserAnimeStatus.WATCHING,
                null,
                null
        );

        mockMvc.perform(post("/api/v1/users/me/animelist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getUserList_shouldReturnList_whenUserExistsAndHasEntries() throws Exception {
        User user = userRepository.findByUsername(USERNAME).get();
        UserAnimeList entry = UserAnimeList.builder()
                .user(user)
                .anime(testAnime)
                .status(UserAnimeStatus.WATCHING)
                .score(8)
                .watchedEpisodes(5)
                .build();
        userAnimeListRepository.save(entry);

        mockMvc.perform(get("/api/v1/users/{username}/animelist", USERNAME)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].animeId").value(testAnime.getId()))
                .andExpect(jsonPath("$[0].status").value("WATCHING"))
                .andExpect(jsonPath("$[0].score").value(8))
                .andExpect(jsonPath("$[0].watchedEpisodes").value(5));
    }

    @Test
    void getUserList_shouldReturnEmptyList_whenUserHasNoEntries() throws Exception {
        mockMvc.perform(get("/api/v1/users/{username}/animelist", USERNAME)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getUserList_shouldFilterByStatus() throws Exception {
        User user = userRepository.findByUsername(USERNAME).get();

        Anime anime2 = Anime.builder()
                .titleRomaji("Attack on Titan")
                .type(AnimeType.TV)
                .episodeCount(87)
                .status(AnimeStatus.FINISHED)
                .build();
        animeRepository.save(anime2);

        UserAnimeList entry1 = UserAnimeList.builder()
                .user(user)
                .anime(testAnime)
                .status(UserAnimeStatus.WATCHING)
                .score(8)
                .watchedEpisodes(5)
                .build();
        userAnimeListRepository.save(entry1);

        UserAnimeList entry2 = UserAnimeList.builder()
                .user(user)
                .anime(anime2)
                .status(UserAnimeStatus.COMPLETED)
                .score(9)
                .watchedEpisodes(87)
                .build();
        userAnimeListRepository.save(entry2);

        mockMvc.perform(get("/api/v1/users/{username}/animelist", USERNAME)
                        .param("status", "WATCHING")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("WATCHING"));
    }

    @Test
    void getUserList_shouldReturnNotFound_whenUserNotExists() throws Exception {
        mockMvc.perform(get("/api/v1/users/{username}/animelist", "NonExistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
    }


    @Test
    void remove_shouldReturnNoContent_whenEntryExists() throws Exception {
        User user = userRepository.findByUsername(USERNAME).get();
        UserAnimeList entry = UserAnimeList.builder()
                .user(user)
                .anime(testAnime)
                .status(UserAnimeStatus.WATCHING)
                .score(8)
                .watchedEpisodes(5)
                .build();
        userAnimeListRepository.save(entry);

        mockMvc.perform(delete("/api/v1/users/me/animelist/{animeId}", testAnime.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void remove_shouldReturnNotFound_whenEntryNotExists() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me/animelist/{animeId}", 999L)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_ANIME_LIST_NOT_FOUND"));
    }

    @Test
    void remove_shouldReturnUnauthorized_whenNoToken() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me/animelist/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void remove_shouldReturnForbidden_whenTokenOfAnotherUser() throws Exception {
        String otherUsername = "OtherUser";
        String otherPassword = "OtherPass";
        User otherUser = User.builder()
                .username(otherUsername)
                .email("other@example.com")
                .password(passwordEncoder.encode(otherPassword))
                .role(Role.ROLE_USER)
                .build();
        userRepository.save(otherUser);

        String otherToken = loginAndGetToken(otherUsername, otherPassword);

        mockMvc.perform(delete("/api/v1/users/me/animelist/{animeId}", testAnime.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
    }
}