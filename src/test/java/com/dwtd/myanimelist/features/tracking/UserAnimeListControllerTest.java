package com.dwtd.myanimelist.features.tracking;

import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.tracking.dto.AddUserAnimeListRequest;
import com.dwtd.myanimelist.features.tracking.dto.UpdateUserAnimeListRequest;
import com.dwtd.myanimelist.features.tracking.enums.UserAnimeStatus;
import com.dwtd.myanimelist.helpers.BaseIntegrationTest;
import com.dwtd.myanimelist.helpers.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserAnimeListControllerTest extends BaseIntegrationTest {

    private static final String BASE_URL = "/api/v1/users/me/animelist";
    private static final String USER_LIST_URL = "/api/v1/users/{username}/animelist";

    private final String USERNAME = "TestUser";
    private final String PASSWORD = "Password123";

    private String userToken;
    private Anime testAnime;

    @BeforeEach
    void setUp() throws Exception {
        saveUser(USERNAME, "test@example.com", PASSWORD, Role.ROLE_USER);
        userToken = loginAndGetToken(USERNAME, PASSWORD);
        testAnime = animeRepository.save(TestDataFactory.anime("Naruto", 220));
    }

    private void createUserAnimeList(User user, Anime anime, UserAnimeStatus status, Integer score, int watchedEpisodes) {
        userAnimeListRepository.save(TestDataFactory.userAnimeList(user, anime, status, score, watchedEpisodes));
    }

    @Test
    void addAnimeToList_shouldReturnCreated_whenValid() throws Exception {
        AddUserAnimeListRequest request = new AddUserAnimeListRequest(testAnime.getId());

        mockMvc.perform(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(userToken))
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
                        .header("Authorization", bearer(userToken))
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
        Anime anime2 = animeRepository.save(TestDataFactory.anime("Attack on Titan", 87));
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
                        .header("Authorization", bearer(userToken))
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
                        .header("Authorization", bearer(userToken))
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
                        .header("Authorization", bearer(userToken))
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
                        .header("Authorization", bearer(userToken))
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
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturnNotFound_whenEntryNotExists() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/{animeId}", 999999L)
                        .header("Authorization", bearer(userToken)))
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

        saveUser("OtherUser", "other@example.com", "OtherPass", Role.ROLE_USER);
        String otherToken = loginAndGetToken("OtherUser", "OtherPass");

        mockMvc.perform(delete(BASE_URL + "/{animeId}", testAnime.getId())
                        .header("Authorization", bearer(otherToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("USER_ANIME_LIST_NOT_FOUND"));
    }
}