package com.dwtd.myanimelist.features.friendlist;

import com.dwtd.myanimelist.features.auth.dto.LoginRequest;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.auth.repository.RefreshTokenRepository;
import com.dwtd.myanimelist.features.auth.repository.UserRepository;
import com.dwtd.myanimelist.features.friendlist.entity.FriendList;
import com.dwtd.myanimelist.features.friendlist.enums.FriendListStatus;
import com.dwtd.myanimelist.features.friendlist.repository.FriendListRepository;
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
public class FriendListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private FriendListRepository friendListRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User alice;
    private User bob;
    private User charlie;

    private String aliceToken;
    private String bobToken;
    private String charlieToken;

    @BeforeEach
    void setUp() throws Exception {
        friendListRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        alice = userRepository.save(User.builder()
                .username("Alice")
                .email("alice@example.com")
                .password(passwordEncoder.encode("Password123"))
                .role(Role.ROLE_USER)
                .build());

        bob = userRepository.save(User.builder()
                .username("Boby")
                .email("bob@example.com")
                .password(passwordEncoder.encode("Password123"))
                .role(Role.ROLE_USER)
                .build());

        charlie = userRepository.save(User.builder()
                .username("Charlie")
                .email("charlie@example.com")
                .password(passwordEncoder.encode("Password123"))
                .role(Role.ROLE_USER)
                .build());

        aliceToken = loginAndGetToken("Alice", "Password123");
        bobToken = loginAndGetToken("Boby", "Password123");
        charlieToken = loginAndGetToken("Charlie", "Password123");
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

    private FriendList createFriendship(User user, User friend, FriendListStatus status) {
        return friendListRepository.save(FriendList.builder()
                .user(user)
                .friend(friend)
                .status(status)
                .build());
    }

    @Test
    void sendFriendRequest_shouldReturnCreated_whenValid() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/friends/{friendId}", bob.getId())
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(alice.getId()))
                .andExpect(jsonPath("$.username").value("Alice"));
    }

    @Test
    void sendFriendRequest_shouldReturnUnauthorized_whenNoToken() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/friends/{friendId}", bob.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void sendFriendRequest_shouldReturnBadRequest_whenAddYourself() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/friends/{friendId}", alice.getId())
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CANNOT_ADD_YOURSELF"));
    }

    @Test
    void sendFriendRequest_shouldReturnConflict_whenAlreadySent() throws Exception {
        createFriendship(alice, bob, FriendListStatus.PENDING);

        mockMvc.perform(post("/api/v1/users/me/friends/{friendId}", bob.getId())
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("FRIEND_REQUEST_ALREADY_EXISTS"));
    }

    @Test
    void getFriends_shouldReturnList() throws Exception {
        createFriendship(alice, bob, FriendListStatus.ACCEPTED);
        createFriendship(alice, charlie, FriendListStatus.ACCEPTED);

        mockMvc.perform(get("/api/v1/users/me/friends")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getFriends_shouldReturnUnauthorized_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/friends"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getIncomingRequests_shouldReturnList() throws Exception {
        createFriendship(alice, bob, FriendListStatus.PENDING);

        mockMvc.perform(get("/api/v1/users/me/friends/requests/incoming")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("Alice"));
    }

    @Test
    void getOutgoingRequests_shouldReturnList() throws Exception {
        createFriendship(alice, bob, FriendListStatus.PENDING);

        mockMvc.perform(get("/api/v1/users/me/friends/requests/outgoing")
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("Boby"));
    }

    @Test
    void acceptFriendRequest_shouldReturnOk_whenReceiver() throws Exception {
        FriendList request = createFriendship(alice, bob, FriendListStatus.PENDING);

        mockMvc.perform(patch("/api/v1/users/me/friends/requests/{id}/accept", request.getId())
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Alice"));
    }

    @Test
    void acceptFriendRequest_shouldReturnForbidden_whenNotReceiver() throws Exception {
        FriendList request = createFriendship(alice, bob, FriendListStatus.PENDING);

        mockMvc.perform(patch("/api/v1/users/me/friends/requests/{id}/accept", request.getId())
                        .header("Authorization", "Bearer " + charlieToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FRIEND_REQUEST_ACCESS_DENIED"));
    }

    @Test
    void rejectFriendRequest_shouldReturnNoContent_whenReceiver() throws Exception {
        FriendList request = createFriendship(alice, bob, FriendListStatus.PENDING);

        mockMvc.perform(patch("/api/v1/users/me/friends/requests/{id}/reject", request.getId())
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancelFriendRequest_shouldReturnNoContent_whenSender() throws Exception {
        FriendList request = createFriendship(alice, bob, FriendListStatus.PENDING);

        mockMvc.perform(delete("/api/v1/users/me/friends/requests/{id}", request.getId())
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeFriend_shouldReturnNoContent_whenFriend() throws Exception {
        createFriendship(alice, bob, FriendListStatus.ACCEPTED);

        mockMvc.perform(delete("/api/v1/users/me/friends/{friendId}", bob.getId())
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeFriend_shouldReturnNotFound_whenNoFriendship() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me/friends/{friendId}", bob.getId())
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("FRIEND_NOT_FOUND"));
    }

    @Test
    void removeFriend_shouldReturnBadRequest_whenAddYourself() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me/friends/{friendId}", alice.getId())
                        .header("Authorization", "Bearer " + aliceToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CANNOT_ADD_YOURSELF"));
    }
}