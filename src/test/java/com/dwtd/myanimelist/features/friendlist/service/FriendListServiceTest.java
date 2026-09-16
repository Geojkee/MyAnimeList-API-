package com.dwtd.myanimelist.features.friendlist.service;

import com.dwtd.myanimelist.exception.friendlist.*;
import com.dwtd.myanimelist.exception.user.UserNotFoundException;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.auth.repository.UserRepository;
import com.dwtd.myanimelist.features.auth.service.UserService;
import com.dwtd.myanimelist.features.friendlist.dto.FriendRequestResponse;
import com.dwtd.myanimelist.features.friendlist.dto.FriendResponse;
import com.dwtd.myanimelist.features.friendlist.entity.FriendList;
import com.dwtd.myanimelist.features.friendlist.enums.FriendListStatus;
import com.dwtd.myanimelist.features.friendlist.repository.FriendListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FriendListServiceTest {

    @Mock
    private FriendListRepository friendListRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private FriendListService friendListService;

    private User currentUser;
    private User friendUser;
    private User adminUser;
    private FriendList pendingRequest;
    private FriendList acceptedFriendship;

    @BeforeEach
    void setUp() {
        currentUser = User.builder()
                .id(1L).username("Alice").role(Role.ROLE_USER).build();

        friendUser = User.builder()
                .id(2L).username("Bob").role(Role.ROLE_USER).build();

        adminUser = User.builder()
                .id(3L).username("Admin").role(Role.ROLE_ADMIN).build();

        pendingRequest = FriendList.builder()
                .id(10L).user(currentUser).friend(friendUser)
                .status(FriendListStatus.PENDING)
                .createdAt(Instant.now())
                .build();

        acceptedFriendship = FriendList.builder()
                .id(11L).user(currentUser).friend(friendUser)
                .status(FriendListStatus.ACCEPTED)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void sendFriendRequest_shouldCreateRequest_whenValid() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(friendUser));
        when(friendListRepository.findBetweenUsers(1L, 2L)).thenReturn(Optional.empty());
        when(friendListRepository.save(any(FriendList.class))).thenReturn(pendingRequest);

        FriendRequestResponse response = friendListService.sendFriendRequest(2L);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("Alice");
        verify(friendListRepository).save(any(FriendList.class));
    }

    @Test
    void sendFriendRequest_shouldThrowCannotAddYourselfException() {
        when(userService.getCurrentUser()).thenReturn(currentUser);

        assertThatThrownBy(() -> friendListService.sendFriendRequest(1L))
                .isInstanceOf(CannotAddYourselfException.class);
        verify(friendListRepository, never()).save(any());
    }

    @Test
    void sendFriendRequest_shouldThrowUserNotFoundException() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendListService.sendFriendRequest(99L))
                .isInstanceOf(UserNotFoundException.class);
        verify(friendListRepository, never()).save(any());
    }

    @Test
    void sendFriendRequest_shouldThrowAlreadyExistsException_whenRequestExists() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(friendUser));
        when(friendListRepository.findBetweenUsers(1L, 2L))
                .thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> friendListService.sendFriendRequest(2L))
                .isInstanceOf(FriendRequestAlreadyExistsException.class);
        verify(friendListRepository, never()).save(any());
    }

    @Test
    void getFriends_shouldReturnListOfFriends() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(friendListRepository.findAllFriendsByUserIdAndStatus(1L, FriendListStatus.ACCEPTED))
                .thenReturn(List.of(acceptedFriendship));

        List<FriendResponse> result = friendListService.getFriends();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(2L);
        assertThat(result.get(0).username()).isEqualTo("Bob");
    }

    @Test
    void getIncomingRequests_shouldReturnList() {
        when(userService.getCurrentUser()).thenReturn(friendUser);
        when(friendListRepository.findAllByFriendIdAndStatus(2L, FriendListStatus.PENDING))
                .thenReturn(List.of(pendingRequest));

        List<FriendRequestResponse> result = friendListService.getIncomingRequests();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(1L);
        assertThat(result.get(0).username()).isEqualTo("Alice");
    }

    @Test
    void getOutgoingRequests_shouldReturnList() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(friendListRepository.findAllByUserIdAndStatus(1L, FriendListStatus.PENDING))
                .thenReturn(List.of(pendingRequest));

        List<FriendRequestResponse> result = friendListService.getOutgoingRequests();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).userId()).isEqualTo(2L);
        assertThat(result.get(0).username()).isEqualTo("Bob");
    }

    @Test
    void acceptFriendRequest_shouldAccept_whenReceiver() {
        when(userService.getCurrentUser()).thenReturn(friendUser);
        when(friendListRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));
        when(friendListRepository.save(pendingRequest)).thenReturn(pendingRequest);

        FriendResponse response = friendListService.acceptFriendRequest(10L);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("Alice");
        assertThat(pendingRequest.getStatus()).isEqualTo(FriendListStatus.ACCEPTED);
        verify(friendListRepository).save(pendingRequest);
    }

    @Test
    void acceptFriendRequest_shouldThrowNotFound() {
        when(userService.getCurrentUser()).thenReturn(friendUser);
        when(friendListRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendListService.acceptFriendRequest(99L))
                .isInstanceOf(FriendRequestNotFoundException.class);
    }

    @Test
    void acceptFriendRequest_shouldThrowAccessDenied_whenNotReceiver() {
        when(userService.getCurrentUser()).thenReturn(adminUser);
        when(friendListRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> friendListService.acceptFriendRequest(10L))
                .isInstanceOf(FriendRequestAccessDeniedException.class);
        verify(friendListRepository, never()).save(any());
    }

    @Test
    void acceptFriendRequest_shouldThrowInvalidStatus_whenAlreadyAccepted() {
        pendingRequest.setStatus(FriendListStatus.ACCEPTED);
        when(userService.getCurrentUser()).thenReturn(friendUser);
        when(friendListRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> friendListService.acceptFriendRequest(10L))
                .isInstanceOf(InvalidFriendRequestStatusException.class);
        verify(friendListRepository, never()).save(any());
    }

    @Test
    void rejectFriendRequest_shouldReject_whenReceiver() {
        when(userService.getCurrentUser()).thenReturn(friendUser);
        when(friendListRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));
        when(friendListRepository.save(pendingRequest)).thenReturn(pendingRequest);

        friendListService.rejectFriendRequest(10L);

        assertThat(pendingRequest.getStatus()).isEqualTo(FriendListStatus.REJECTED);
        verify(friendListRepository).save(pendingRequest);
    }

    @Test
    void rejectFriendRequest_shouldThrowNotFound() {
        when(userService.getCurrentUser()).thenReturn(friendUser);
        when(friendListRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendListService.rejectFriendRequest(99L))
                .isInstanceOf(FriendRequestNotFoundException.class);
    }

    @Test
    void rejectFriendRequest_shouldThrowAccessDenied_whenNotReceiver() {
        when(userService.getCurrentUser()).thenReturn(adminUser);
        when(friendListRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> friendListService.rejectFriendRequest(10L))
                .isInstanceOf(FriendRequestAccessDeniedException.class);
    }

    @Test
    void cancelFriendRequest_shouldDelete_whenSender() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(friendListRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));

        friendListService.cancelFriendRequest(10L);

        verify(friendListRepository).delete(pendingRequest);
    }

    @Test
    void cancelFriendRequest_shouldThrowNotFound() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(friendListRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendListService.cancelFriendRequest(99L))
                .isInstanceOf(FriendRequestNotFoundException.class);
    }

    @Test
    void cancelFriendRequest_shouldThrowAccessDenied_whenNotSender() {
        when(userService.getCurrentUser()).thenReturn(friendUser);
        when(friendListRepository.findById(10L)).thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> friendListService.cancelFriendRequest(10L))
                .isInstanceOf(FriendRequestAccessDeniedException.class);
        verify(friendListRepository, never()).delete(any());
    }

    @Test
    void removeFriend_shouldDelete_whenAcceptedFriendship() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(friendListRepository.findBetweenUsers(1L, 2L))
                .thenReturn(Optional.of(acceptedFriendship));

        friendListService.removeFriend(2L);

        verify(friendListRepository).delete(acceptedFriendship);
    }

    @Test
    void removeFriend_shouldThrowCannotAddYourself() {
        when(userService.getCurrentUser()).thenReturn(currentUser);

        assertThatThrownBy(() -> friendListService.removeFriend(1L))
                .isInstanceOf(CannotAddYourselfException.class);
        verify(friendListRepository, never()).delete(any());
    }

    @Test
    void removeFriend_shouldThrowNotFound_whenNoRelation() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(friendListRepository.findBetweenUsers(1L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> friendListService.removeFriend(2L))
                .isInstanceOf(FriendNotFoundException.class);
    }

    @Test
    void removeFriend_shouldThrowNotFound_whenNotAccepted() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(friendListRepository.findBetweenUsers(1L, 2L))
                .thenReturn(Optional.of(pendingRequest));

        assertThatThrownBy(() -> friendListService.removeFriend(2L))
                .isInstanceOf(FriendNotFoundException.class);
        verify(friendListRepository, never()).delete(any());
    }
}