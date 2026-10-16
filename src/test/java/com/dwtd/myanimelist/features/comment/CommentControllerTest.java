package com.dwtd.myanimelist.features.comment;

import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.comment.entity.Comment;
import com.dwtd.myanimelist.helpers.BaseIntegrationTest;
import com.dwtd.myanimelist.helpers.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CommentControllerTest extends BaseIntegrationTest {

    private final String ADMIN_USERNAME = "Admin";
    private final String ADMIN_PASSWORD = "AdminPassword";
    private final String USER_USERNAME = "User";
    private final String USER_PASSWORD = "UserPassword";

    private String adminToken;
    private String userToken;
    private Anime testAnime;
    private Comment userComment;
    private Comment otherUserComment;

    @BeforeEach
    void setUp() throws Exception {
        saveUser(ADMIN_USERNAME, "admin@example.com", ADMIN_PASSWORD, Role.ROLE_ADMIN);
        User user = saveUser(USER_USERNAME, "user@example.com", USER_PASSWORD, Role.ROLE_USER);
        User otherUser = saveUser("OtherUser", "other@example.com", "OtherPassword", Role.ROLE_USER);

        testAnime = animeRepository.save(TestDataFactory.anime("Naruto", 220));
        userComment = commentRepository.save(TestDataFactory.comment(testAnime, user, "Perfect"));
        otherUserComment = commentRepository.save(TestDataFactory.comment(testAnime, otherUser, "Good"));

        adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        userToken = loginAndGetToken(USER_USERNAME, USER_PASSWORD);
    }

    private String validCommentJson() {
        return """
                {
                    "text": "Amazing"
                }
                """;
    }

    private String invalidCommentJson() {
        return """
                {
                    "text": ""
                }
                """;
    }

    @Test
    void createComment_shouldReturnCreated_whenAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/anime/{id}/comments", testAnime.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(userToken))
                        .content(validCommentJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Amazing"))
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.username").value(USER_USERNAME));
    }

    @Test
    void createComment_shouldReturnUnauthorized_whenNoToken() throws Exception {
        mockMvc.perform(post("/api/v1/anime/{id}/comments", testAnime.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCommentJson()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void createComment_shouldReturnBadRequest_whenInvalidData() throws Exception {
        mockMvc.perform(post("/api/v1/anime/{id}/comments", testAnime.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(userToken))
                        .content(invalidCommentJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void createComment_shouldReturnNotFound_whenAnimeNotExists() throws Exception {
        mockMvc.perform(post("/api/v1/anime/999999/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", bearer(userToken))
                        .content(validCommentJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ANIME_NOT_FOUND"));
    }

    @Test
    void getComments_shouldReturnPageOfComments_whenAnimeExists() throws Exception {
        mockMvc.perform(get("/api/v1/anime/{id}/comments", testAnime.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getComments_shouldReturnNotFound_whenAnimeNotExists() throws Exception {
        mockMvc.perform(get("/api/v1/anime/999999/comments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ANIME_NOT_FOUND"));
    }

    @Test
    void deleteComment_shouldReturnNoContent_whenAuthor() throws Exception {
        mockMvc.perform(delete("/api/v1/anime/{animeId}/comments/{commentId}", testAnime.getId(), userComment.getId())
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteComment_shouldReturnNoContent_whenAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/anime/{animeId}/comments/{commentId}", testAnime.getId(), userComment.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteComment_shouldReturnUnauthorized_whenNoToken() throws Exception {
        mockMvc.perform(delete("/api/v1/anime/{animeId}/comments/{commentId}", testAnime.getId(), userComment.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void deleteComment_shouldReturnForbidden_whenNotAuthorAndNotAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/anime/{animeId}/comments/{commentId}", testAnime.getId(), otherUserComment.getId())
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_ACCESS_DENIED"));
    }

    @Test
    void deleteComment_shouldReturnNotFound_whenCommentNotExists() throws Exception {
        mockMvc.perform(delete("/api/v1/anime/{animeId}/comments/{commentId}", testAnime.getId(), 999999L)
                        .header("Authorization", bearer(userToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_NOT_FOUND"));
    }
}
