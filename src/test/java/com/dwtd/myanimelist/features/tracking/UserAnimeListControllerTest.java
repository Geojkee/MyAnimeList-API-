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
import com.dwtd.myanimelist.features.comment.repository.CommentRepository;
import com.dwtd.myanimelist.features.tracking.dto.AddUserAnimeListRequest;
import com.dwtd.myanimelist.features.tracking.dto.UpdateUserAnimeListRequest;
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

    private static final String BASE_URL = "/api/v1/users/me/animelist";
    private static final String USER_LIST_URL = "/api/v1/users/{username}/animelist";

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

    @Autowired
    private CommentRepository commentRepository;

    private final String USERNAME = "TestUser";
    private final String PASSWORD = "Password123";
    private final String EMAIL = "test@example.com";

    private String userToken;
    private Anime testAnime;

    @BeforeEach
    void setUp() throws Exception {
        commentRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userAnimeListRepository.deleteAll();
        animeRepository.deleteAll();
        userRepository.deleteAll();

        createUser(USERNAME, PASSWORD, EMAIL);
        userToken = loginAndGetToken(USERNAME, PASSWORD);
        testAnime = createAnime("Naruto", 220);
    }

    private void createUser(String username, String password, String email) {
        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.ROLE_USER)
                .build();
        userRepository.save(user);
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

    private Anime createAnime(String title, int episodeCount) {
        Anime anime = Anime.builder()
                .titleRomaji(title)
                .type(AnimeType.TV)
                .episodeCount(episodeCount)
                .status(AnimeStatus.FINISHED)
                .build();
        return animeRepository.save(anime);
    }

    private void createUserAnimeList(User user, Anime anime, UserAnimeStatus status, Integer score, int watchedEpisodes) {
        UserAnimeList entry = UserAnimeList.builder()
                .user(user)
                .anime(anime)
                .status(status)
                .score(score)
                .watchedEpisodes(watchedEpisodes)
                .build();
        userAnimeListRepository.save(entry);
    }

    @Test
    void addAnimeToList_shouldReturnCreated_whenValid() throws Exception {
        AddUserAnimeListRequest request = new AddUserAnimeListRequest(testAnime.getId());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.animeId").value(testAnime.getId()))
                .andExpect(jsonPath("$.status").value("WATCHING"))
                .andExpect(jsonPath("$.score").doesNotExist())
                .andExpect(jsonPath("$.watchedEpisodes").value(0));
    }

    @Test
    void addAnimeToList_shouldReturnConflict_whenAnimeAlreadyInList() throws Exception {
        User user = userRepository.findByUsername(USERNAME).orElseThrow();
        createUserAnimeList(user, testAnime, UserAnimeStatus.WATCHING, null, 0);

        AddUserAnimeListRequest request = new AddUserAnimeListRequest(testAnime.getId());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ANIME_ALREADY_EXISTS_IN_USER_LIST"));
    }

    @Test
    void addAnimeToList_shouldReturnUnauthorized_whenNoToken() throws Exception {
        AddUserAnimeListRequest request = new AddUserAnimeListRequest(testAnime.getId());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void getUserList_shouldReturnList_whenUserHasEntries() throws Exception {
        User user = userRepository.findByUsername(USERNAME).orElseThrow();
        createUserAnimeList(user, testAnime, UserAnimeStatus.WATCHING, 8, 5);

        mockMvc.perform(get(USER_LIST_URL, USERNAME)
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
        mockMvc.perform(get(USER_LIST_URL, USERNAME)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getUserList_shouldFilterByStatus() throws Exception {
        User user = userRepository.findByUsername(USERNAME).orElseThrow();

        Anime anime2 = createAnime("Attack on Titan", 87);
        createUserAnimeList(user, testAnime, UserAnimeStatus.WATCHING, 8, 5);
        createUserAnimeList(user, anime2, UserAnimeStatus.COMPLETED, 9, 87);

        mockMvc.perform(get(USER_LIST_URL, USERNAME)
                        .param("status", "WATCHING")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("WATCHING"));
    }

    @Test
    void getUserList_shouldReturnNotFound_whenUserNotExists() throws Exception {
        mockMvc.perform(get(USER_LIST_URL, "NonExistent")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_NOT_FOUND"));
    }

    @Test
    void updateAnimeInList_shouldReturnOk_whenValid() throws Exception {
        User user = userRepository.findByUsername(USERNAME).orElseThrow();
        createUserAnimeList(user, testAnime, UserAnimeStatus.WATCHING, 8, 5);

        UpdateUserAnimeListRequest request = new UpdateUserAnimeListRequest(
                UserAnimeStatus.COMPLETED, 10, 220
        );

        mockMvc.perform(patch(BASE_URL + "/{animeId}", testAnime.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.score").value(10))
                .andExpect(jsonPath("$.watchedEpisodes").value(220));
    }

    @Test
    void updateAnimeInList_shouldAutoCompleteWatched_whenStatusCompletedAndWatchedNotProvided() throws Exception {
        User user = userRepository.findByUsername(USERNAME).orElseThrow();
        createUserAnimeList(user, testAnime, UserAnimeStatus.WATCHING, 8, 5);

        UpdateUserAnimeListRequest request = new UpdateUserAnimeListRequest(
                UserAnimeStatus.COMPLETED, null, null
        );

        mockMvc.perform(patch(BASE_URL + "/{animeId}", testAnime.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.watchedEpisodes").value(220));
    }

    @Test
    void updateAnimeInList_shouldReturnBadRequest_whenWatchedExceedsTotal() throws Exception {
        User user = userRepository.findByUsername(USERNAME).orElseThrow();
        createUserAnimeList(user, testAnime, UserAnimeStatus.WATCHING, 8, 5);

        UpdateUserAnimeListRequest request = new UpdateUserAnimeListRequest(
                UserAnimeStatus.WATCHING, null, 300
        );

        mockMvc.perform(patch(BASE_URL + "/{animeId}", testAnime.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_USER_ANIME_DATA"));
    }

    @Test
    void updateAnimeInList_shouldReturnNotFound_whenAnimeNotInList() throws Exception {
        UpdateUserAnimeListRequest request = new UpdateUserAnimeListRequest(
                UserAnimeStatus.COMPLETED, 10, 220
        );

        mockMvc.perform(patch(BASE_URL + "/{animeId}", testAnime.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_ANIME_LIST_NOT_FOUND"));
    }

    @Test
    void updateAnimeInList_shouldReturnUnauthorized_whenNoToken() throws Exception {
        UpdateUserAnimeListRequest request = new UpdateUserAnimeListRequest(
                UserAnimeStatus.COMPLETED, 10, 220
        );

        mockMvc.perform(patch(BASE_URL + "/{animeId}", testAnime.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void delete_shouldReturnNoContent_whenEntryExists() throws Exception {
        User user = userRepository.findByUsername(USERNAME).orElseThrow();
        createUserAnimeList(user, testAnime, UserAnimeStatus.WATCHING, 8, 5);

        mockMvc.perform(delete(BASE_URL + "/{animeId}", testAnime.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturnNotFound_whenEntryNotExists() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{animeId}", 999L)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_ANIME_LIST_NOT_FOUND"));
    }

    @Test
    void delete_shouldReturnUnauthorized_whenNoToken() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{animeId}", testAnime.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void delete_shouldReturnNotFound_whenAnotherUserTriesToDelete() throws Exception {
        User testUser = userRepository.findByUsername(USERNAME).orElseThrow();
        createUserAnimeList(testUser, testAnime, UserAnimeStatus.WATCHING, 8, 5);

        String otherUsername = "OtherUser";
        String otherPassword = "OtherPass";
        createUser(otherUsername, otherPassword, "other@example.com");
        String otherToken = loginAndGetToken(otherUsername, otherPassword);

        mockMvc.perform(delete(BASE_URL + "/{animeId}", testAnime.getId())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_ANIME_LIST_NOT_FOUND"));
    }
}