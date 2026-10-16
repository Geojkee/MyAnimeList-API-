package com.dwtd.myanimelist.features.friendlist;

import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.friendlist.entity.FriendList;
import com.dwtd.myanimelist.features.friendlist.enums.FriendListStatus;
import com.dwtd.myanimelist.helpers.BaseIntegrationTest;
import com.dwtd.myanimelist.helpers.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FriendListControllerTest extends BaseIntegrationTest {

    private User alice;
    private User bob;
    private User charlie;

    private String aliceToken;
    private String bobToken;
    private String charlieToken;

    @BeforeEach
    void setUp() throws Exception {
        alice = saveUser("Alice", "alice@example.com", "Password123", Role.ROLE_USER);
        bob = saveUser("Boby", "bob@example.com", "Password123", Role.ROLE_USER);
        charlie = saveUser("Charlie", "charlie@example.com", "Password123", Role.ROLE_USER);

        aliceToken = loginAndGetToken("Alice", "Password123");
        bobToken = loginAndGetToken("Boby", "Password123");
        charlieToken = loginAndGetToken("Charlie", "Password123");
    }

    private FriendList createFriendship(User user, User friend, FriendListStatus status) {
        return friendListRepository.save(TestDataFactory.friendship(user, friend, status));
    }

    @Test
    void sendFriendRequest_shouldReturnCreated_whenValid() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/friends/{friendId}", bob.getId())
                        .header("Authorization", bearer(aliceToken)))
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
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CANNOT_ADD_YOURSELF"));
    }

    @Test
    void sendFriendRequest_shouldReturnConflict_whenAlreadySent() throws Exception {
        createFriendship(alice, bob, FriendListStatus.PENDING);

        mockMvc.perform(post("/api/v1/users/me/friends/{friendId}", bob.getId())
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("FRIEND_REQUEST_ALREADY_EXISTS"));
    }

    @Test
    void getFriends_shouldReturnList() throws Exception {
        createFriendship(alice, bob, FriendListStatus.ACCEPTED);
        createFriendship(alice, charlie, FriendListStatus.ACCEPTED);

        mockMvc.perform(get("/api/v1/users/me/friends")
                        .header("Authorization", bearer(aliceToken)))
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
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("Alice"));
    }

    @Test
    void getOutgoingRequests_shouldReturnList() throws Exception {
        createFriendship(alice, bob, FriendListStatus.PENDING);

        mockMvc.perform(get("/api/v1/users/me/friends/requests/outgoing")
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].username").value("Boby"));
    }

    @Test
    void acceptFriendRequest_shouldReturnOk_whenReceiver() throws Exception {
        FriendList request = createFriendship(alice, bob, FriendListStatus.PENDING);

        mockMvc.perform(patch("/api/v1/users/me/friends/requests/{id}/accept", request.getId())
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("Alice"));
    }

    @Test
    void acceptFriendRequest_shouldReturnForbidden_whenNotReceiver() throws Exception {
        FriendList request = createFriendship(alice, bob, FriendListStatus.PENDING);

        mockMvc.perform(patch("/api/v1/users/me/friends/requests/{id}/accept", request.getId())
                        .header("Authorization", bearer(charlieToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FRIEND_REQUEST_ACCESS_DENIED"));
    }

    @Test
    void rejectFriendRequest_shouldReturnNoContent_whenReceiver() throws Exception {
        FriendList request = createFriendship(alice, bob, FriendListStatus.PENDING);

        mockMvc.perform(patch("/api/v1/users/me/friends/requests/{id}/reject", request.getId())
                        .header("Authorization", bearer(bobToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancelFriendRequest_shouldReturnNoContent_whenSender() throws Exception {
        FriendList request = createFriendship(alice, bob, FriendListStatus.PENDING);

        mockMvc.perform(delete("/api/v1/users/me/friends/requests/{id}", request.getId())
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeFriend_shouldReturnNoContent_whenFriend() throws Exception {
        createFriendship(alice, bob, FriendListStatus.ACCEPTED);

        mockMvc.perform(delete("/api/v1/users/me/friends/{friendId}", bob.getId())
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeFriend_shouldReturnNotFound_whenNoFriendship() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me/friends/{friendId}", bob.getId())
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("FRIEND_NOT_FOUND"));
    }

    @Test
    void removeFriend_shouldReturnBadRequest_whenAddYourself() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me/friends/{friendId}", alice.getId())
                        .header("Authorization", bearer(aliceToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("CANNOT_ADD_YOURSELF"));
    }
}
