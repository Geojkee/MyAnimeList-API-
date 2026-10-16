package com.dwtd.myanimelist.helpers;

import com.dwtd.myanimelist.features.anime.repository.AnimeRepository;
import com.dwtd.myanimelist.features.auth.dto.LoginRequest;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.auth.repository.RefreshTokenRepository;
import com.dwtd.myanimelist.features.auth.repository.UserRepository;
import com.dwtd.myanimelist.features.comment.repository.CommentRepository;
import com.dwtd.myanimelist.features.friendlist.repository.FriendListRepository;
import com.dwtd.myanimelist.features.genre.repository.GenreRepository;
import com.dwtd.myanimelist.features.tracking.repository.UserAnimeListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected CommentRepository commentRepository;

    @Autowired
    protected FriendListRepository friendListRepository;

    @Autowired
    protected UserAnimeListRepository userAnimeListRepository;

    @Autowired
    protected AnimeRepository animeRepository;

    @Autowired
    protected GenreRepository genreRepository;

    @BeforeEach
    protected void baseSetUp() {
        commentRepository.deleteAll();
        friendListRepository.deleteAll();
        userAnimeListRepository.deleteAll();
        animeRepository.deleteAll();
        genreRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected User saveUser(String username, String email, String rawPassword, Role role) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .build());
    }

    protected String loginAndGetToken(String username, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult result = mockMvc.perform(
                        post("/api/v1/auth/sign-in")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();
        String json = result.getResponse().getContentAsString();
        return objectMapper.readTree(json).get("token").asText();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }
}
