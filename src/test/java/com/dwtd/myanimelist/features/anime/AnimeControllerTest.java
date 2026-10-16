package com.dwtd.myanimelist.features.anime;

import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.anime.enums.AnimeStatus;
import com.dwtd.myanimelist.features.anime.enums.AnimeType;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.genre.entity.Genre;
import com.dwtd.myanimelist.helpers.BaseIntegrationTest;
import com.dwtd.myanimelist.helpers.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AnimeControllerTest extends BaseIntegrationTest {

    private final String ADMIN_USERNAME = "Admin";
    private final String ADMIN_PASSWORD = "AdminPassword";
    private final String USER_USERNAME = "User";
    private final String USER_PASSWORD = "UserPassword";

    private String adminToken;
    private String userToken;
    private Anime testAnime1;
    private Anime testAnime2;
    private Anime testAnime3;
    private Anime testAnime4;
    private Long romanceGenreId;
    private Long adventureGenreId;
    private Long comedyGenreId;

    @BeforeEach
    void setUp() throws Exception {
        saveUser(ADMIN_USERNAME, "admin@example.com", ADMIN_PASSWORD, Role.ROLE_ADMIN);
        saveUser(USER_USERNAME, "user@example.com", USER_PASSWORD, Role.ROLE_USER);

        adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        userToken = loginAndGetToken(USER_USERNAME, USER_PASSWORD);

        testAnime1 = animeRepository.save(TestDataFactory.anime("Naruto", 220));
        testAnime1.setTitleEnglish("Naruto English");
        testAnime1 = animeRepository.save(testAnime1);

        testAnime2 = animeRepository.save(TestDataFactory.anime("Attack on Titan", 87));
        testAnime3 = animeRepository.save(TestDataFactory.anime("Wasted Chef", AnimeType.MOVIE, 1, AnimeStatus.ANNOUNCED));
        testAnime4 = animeRepository.save(TestDataFactory.anime("Mushoku Tensei", AnimeType.TV, 3, AnimeStatus.ONGOING));

        Genre romance = genreRepository.save(TestDataFactory.genre("romance"));
        Genre adventure = genreRepository.save(TestDataFactory.genre("adventure"));
        Genre comedy = genreRepository.save(TestDataFactory.genre("comedy"));
        romanceGenreId = romance.getId();
        adventureGenreId = adventure.getId();
        comedyGenreId = comedy.getId();

        testAnime1.setGenres(Set.of(romance));
        testAnime1 = animeRepository.save(testAnime1);
    }

    private String validAnimeJson() {
        return """
                {
                    "titleRomaji": "Test Anime",
                    "titleEnglish": "Test Anime English",
                    "type": "TV",
                    "episodeCount": 12,
                    "status": "FINISHED",
                    "synopsis": "Test synopsis",
                    "genreIds": [%d, %d, %d]
                }
                """.formatted(romanceGenreId, adventureGenreId, comedyGenreId);
    }

    private String invalidAnimeJson() {
        return """
                {
                    "titleRomaji": "",
                    "type": "TV",
                    "episodeCount": -5,
                    "status": "FINISHED",
                    "genreIds": [%d, %d, %d]
                }
                """.formatted(romanceGenreId, adventureGenreId, comedyGenreId);
    }

    private String updateAnimeJson() {
        return """
                {
                    "titleRomaji": "Naruto (update)",
                    "titleEnglish": "Naruto English (update)",
                    "type": "TV",
                    "episodeCount": 221,
                    "status": "FINISHED",
                    "synopsis": "Naruto synopsis (update)",
                    "genreIds": [%d, %d, %d]
                }
                """.formatted(romanceGenreId, adventureGenreId, comedyGenreId);
    }

    @Test
    void create_anime_shouldReturnCreated_whenAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/anime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
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
        mockMvc.perform(post("/api/v1/anime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(userToken))
                        .content(validAnimeJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_shouldReturnUnauthorized_whenNoToken() throws Exception {
        mockMvc.perform(post("/api/v1/anime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validAnimeJson()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_shouldReturnBadRequest_whenInvalidData() throws Exception {
        mockMvc.perform(post("/api/v1/anime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content(invalidAnimeJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void create_shouldReturnConflict_whenDuplicateTitle() throws Exception {
        mockMvc.perform(post("/api/v1/anime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content(validAnimeJson()))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/anime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content(validAnimeJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ANIME_ALREADY_EXISTS"));
    }

    @Test
    void create_shouldReturnBadRequest_whenGenreIdsInvalid() throws Exception {
        String json = """
                {
                    "titleRomaji": "Unknown Genre Anime",
                    "type": "TV",
                    "episodeCount": 12,
                    "status": "FINISHED",
                    "genreIds": [999999]
                }
                """;

        mockMvc.perform(post("/api/v1/anime")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_GENRE_IDS"));
    }

    @Test
    void getAll_shouldReturnPageOfAnime_whenPublic() throws Exception {
        mockMvc.perform(get("/api/v1/anime")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(4));
    }

    @Test
    void getById_shouldReturnAnime_whenExists() throws Exception {
        mockMvc.perform(get("/api/v1/anime/{id}", testAnime1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testAnime1.getId()))
                .andExpect(jsonPath("$.titleRomaji").value("Naruto"));
    }

    @Test
    void getById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/anime/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ANIME_NOT_FOUND"));
    }

    @Test
    void update_anime_shouldReturnOk_whenAdmin() throws Exception {
        mockMvc.perform(put("/api/v1/anime/{id}", testAnime1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
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
        mockMvc.perform(put("/api/v1/anime/{id}", testAnime1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(userToken))
                        .content(updateAnimeJson()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void update_anime_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(put("/api/v1/anime/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content(updateAnimeJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ANIME_NOT_FOUND"));
    }

    @Test
    void update_shouldReturnConflict_whenDuplicateTitle() throws Exception {
        String updateJson = """
            {
                "titleRomaji": "Naruto",
                "type": "TV",
                "episodeCount": 1,
                "status": "FINISHED",
                "genreIds": [%d, %d, %d]
            }
            """.formatted(romanceGenreId, adventureGenreId, comedyGenreId);

        mockMvc.perform(put("/api/v1/anime/{id}", testAnime2.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken))
                        .content(updateJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ANIME_ALREADY_EXISTS"));
    }

    @Test
    void delete_shouldReturnNoContent_whenAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/anime/{id}", testAnime1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturnForbidden_whenUser() throws Exception {
        mockMvc.perform(delete("/api/v1/anime/{id}", testAnime1.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    void delete_shouldReturnNotFound_whenNotExists() throws Exception {
        mockMvc.perform(delete("/api/v1/anime/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ANIME_NOT_FOUND"));
    }

    @Test
    void delete_shouldReturnUnauthorized_whenNoToken() throws Exception {
        mockMvc.perform(delete("/api/v1/anime/{id}", testAnime1.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAll_shouldFilterBySearch() throws Exception {
        mockMvc.perform(get("/api/v1/anime")
                        .param("search", "Titan")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titleRomaji").value("Attack on Titan"));
    }

    @Test
    void getAll_shouldFilterByEnglishTitle() throws Exception {
        mockMvc.perform(get("/api/v1/anime")
                        .param("search", "Naruto English")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titleRomaji").value("Naruto"));
    }

    @Test
    void getAll_shouldFilterByType() throws Exception {
        mockMvc.perform(get("/api/v1/anime")
                        .param("type", "MOVIE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titleRomaji").value("Wasted Chef"));
    }

    @Test
    void getAll_shouldFilterByStatus() throws Exception {
        mockMvc.perform(get("/api/v1/anime")
                        .param("status", "FINISHED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].titleRomaji").value("Naruto"))
                .andExpect(jsonPath("$.content[1].titleRomaji").value("Attack on Titan"));
    }

    @Test
    void getAll_shouldFilterByGenre() throws Exception {
        mockMvc.perform(get("/api/v1/anime")
                        .param("genreId", romanceGenreId.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titleRomaji").value("Naruto"));
    }

    @Test
    void getAll_shouldFilterByCombination() throws Exception {
        mockMvc.perform(get("/api/v1/anime")
                        .param("type", "TV")
                        .param("status", "ONGOING")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].titleRomaji").value("Mushoku Tensei"));
    }
}
