package com.dwtd.myanimelist.features.comment;

import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.anime.enums.AnimeStatus;
import com.dwtd.myanimelist.features.anime.enums.AnimeType;
import com.dwtd.myanimelist.features.anime.repository.AnimeRepository;
import com.dwtd.myanimelist.features.auth.dto.LoginRequest;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.auth.repository.RefreshTokenRepository;
import com.dwtd.myanimelist.features.auth.repository.UserRepository;
import com.dwtd.myanimelist.features.comment.entity.Comment;
import com.dwtd.myanimelist.features.comment.repository.CommentRepository;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CommentControllerTest {

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
    private CommentRepository commentRepository;

    @Autowired
    private AnimeRepository animeRepository;

    private final String ADMIN_USERNAME = "Admin";
    private final String ADMIN_EMAIL = "admin@example.com";
    private final String ADMIN_PASSWORD = "AdminPassword";

    private final String USER_USERNAME = "User";
    private final String USER_EMAIL = "user@example.com";
    private final String USER_PASSWORD = "UserPassword";

    private final String OTHER_USERNAME = "OtherUser";
    private final String OTHER_EMAIL = "other@example.com";
    private final String OTHER_PASSWORD = "OtherPassword";

    private String adminToken;
    private String userToken;
    private String otherUserToken;

    private Anime testAnime;
    private Comment userComment;
    private Comment otherUserComment;

    @BeforeEach
    void setUp() throws Exception {
        commentRepository.deleteAll();
        animeRepository.deleteAll();
        refreshTokenRepository.deleteAll();
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

        User otherUser = User.builder()
                .username(OTHER_USERNAME)
                .email(OTHER_EMAIL)
                .password(passwordEncoder.encode(OTHER_PASSWORD))
                .role(Role.ROLE_USER)
                .build();
        userRepository.save(otherUser);

        testAnime = Anime.builder()
                .titleRomaji("Naruto")
                .type(AnimeType.TV)
                .episodeCount(220)
                .status(AnimeStatus.FINISHED)
                .build();
        animeRepository.save(testAnime);

        userComment = Comment.builder()
                .anime(testAnime)
                .user(user)
                .text("Perfect")
                .build();
        commentRepository.save(userComment);

        otherUserComment = Comment.builder()
                .anime(testAnime)
                .user(otherUser)
                .text("Good")
                .build();
        commentRepository.save(otherUserComment);

        adminToken = loginAndGetToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        userToken = loginAndGetToken(USER_USERNAME, USER_PASSWORD);
        otherUserToken = loginAndGetToken(OTHER_USERNAME, OTHER_PASSWORD);
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
                        .header("Authorization", "Bearer " + userToken)
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
                        .header("Authorization", "Bearer " + userToken)
                        .content(invalidCommentJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    void createComment_shouldReturnNotFound_whenAnimeNotExists() throws Exception {
        mockMvc.perform(post("/api/v1/anime/99/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken)
                        .content(validCommentJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ANIME_NOT_FOUND"));
    }

    @Test
    void getComments_shouldReturnPageOfComments_whenAnimeExists() throws Exception {
        mockMvc.perform(get("/api/v1/anime/{id}/comments", testAnime.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void getComments_shouldReturnNotFound_whenAnimeNotExists() throws Exception {
        mockMvc.perform(get("/api/v1/anime/99/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ANIME_NOT_FOUND"));
    }

    @Test
    void deleteComment_shouldReturnNoContent_whenAuthor() throws Exception {
        mockMvc.perform(delete("/api/v1/anime/{animeId}/comments/{commentId}", testAnime.getId(), userComment.getId())
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteComment_shouldReturnNoContent_whenAdmin() throws Exception {
        mockMvc.perform(delete("/api/v1/anime/{animeId}/comments/{commentId}", testAnime.getId(), userComment.getId())
                        .header("Authorization", "Bearer " + adminToken))
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
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_ACCESS_DENIED"));
    }

    @Test
    void deleteComment_shouldReturnNotFound_whenCommentNotExists() throws Exception {
        mockMvc.perform(delete("/api/v1/anime/{animeId}/comments/{commentId}", testAnime.getId(), 999L)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("COMMENT_NOT_FOUND"));
    }
}