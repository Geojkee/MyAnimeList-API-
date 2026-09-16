package com.dwtd.myanimelist.features.friendlist.service;

import com.dwtd.myanimelist.exception.friendlist.*;
import com.dwtd.myanimelist.exception.user.UserNotFoundException;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.repository.UserRepository;
import com.dwtd.myanimelist.features.auth.service.UserService;
import com.dwtd.myanimelist.features.friendlist.dto.FriendRequestResponse;
import com.dwtd.myanimelist.features.friendlist.dto.FriendResponse;
import com.dwtd.myanimelist.features.friendlist.entity.FriendList;
import com.dwtd.myanimelist.features.friendlist.enums.FriendListStatus;
import com.dwtd.myanimelist.features.friendlist.repository.FriendListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendListService {

    private final FriendListRepository friendListRepository;

    private final UserRepository userRepository;

    private final UserService userService;

    @Transactional
    public FriendRequestResponse sendFriendRequest(Long friendId) {
        User currentUser = userService.getCurrentUser();

        if (currentUser.getId().equals(friendId)) {
            throw new CannotAddYourselfException();
        }

        User friend = userRepository.findById(friendId)
                .orElseThrow(() -> new UserNotFoundException(friendId.toString()));

        friendListRepository.findBetweenUsers(currentUser.getId(), friendId)
                .ifPresent(existing -> {
                    throw new FriendRequestAlreadyExistsException(friendId);
                });

        FriendList friendList = FriendList.builder()
                .user(currentUser)
                .friend(friend)
                .status(FriendListStatus.PENDING)
                .build();

        FriendList saved = friendListRepository.save(friendList);
        log.info("Friend request sent: from={} to={}", currentUser.getUsername(), friend.getUsername());

        return FriendRequestResponse.builder()
                .requestId(saved.getId())
                .userId(currentUser.getId())
                .username(currentUser.getUsername())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> getFriends() {
        User currentUser = userService.getCurrentUser();

        return friendListRepository
                .findAllFriendsByUserIdAndStatus(currentUser.getId(), FriendListStatus.ACCEPTED)
                .stream()
                .map(f -> {
                    User friend = f.getUser().getId().equals(currentUser.getId())
                            ? f.getFriend()
                            : f.getUser();
                    return mapToFriendResponse(friend);
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> getIncomingRequests() {
        User currentUser = userService.getCurrentUser();

        return friendListRepository
                .findAllByFriendIdAndStatus(currentUser.getId(), FriendListStatus.PENDING)
                .stream()
                .map(this::mapToRequestResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> getOutgoingRequests() {
        User currentUser = userService.getCurrentUser();

        return friendListRepository
                .findAllByUserIdAndStatus(currentUser.getId(), FriendListStatus.PENDING)
                .stream()
                .map(f -> FriendRequestResponse.builder()
                        .requestId(f.getId())
                        .userId(f.getFriend().getId())
                        .username(f.getFriend().getUsername())
                        .createdAt(f.getCreatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public FriendResponse acceptFriendRequest(Long requestId) {
        User currentUser = userService.getCurrentUser();

        FriendList request = friendListRepository.findById(requestId)
                .orElseThrow(() -> new FriendRequestNotFoundException(requestId));

        if (!request.getFriend().getId().equals(currentUser.getId())) {
            throw new FriendRequestAccessDeniedException();
        }

        if (request.getStatus() != FriendListStatus.PENDING) {
            throw new InvalidFriendRequestStatusException(request.getStatus().name());
        }

        request.setStatus(FriendListStatus.ACCEPTED);
        FriendList saved = friendListRepository.save(request);
        log.info("Friend request accepted: from={} by={}",
                saved.getUser().getUsername(), currentUser.getUsername());

        return mapToFriendResponse(saved.getUser());
    }

    @Transactional
    public void rejectFriendRequest(Long requestId) {
        User currentUser = userService.getCurrentUser();

        FriendList request = friendListRepository.findById(requestId)
                .orElseThrow(() -> new FriendRequestNotFoundException(requestId));

        if (!request.getFriend().getId().equals(currentUser.getId())) {
            throw new FriendRequestAccessDeniedException();
        }

        if (request.getStatus() != FriendListStatus.PENDING) {
            throw new InvalidFriendRequestStatusException(request.getStatus().name());
        }

        request.setStatus(FriendListStatus.REJECTED);
        friendListRepository.save(request);
        log.info("Friend request rejected: id={}", requestId);
    }

    @Transactional
    public void cancelFriendRequest(Long requestId) {
        User currentUser = userService.getCurrentUser();

        FriendList request = friendListRepository.findById(requestId)
                .orElseThrow(() -> new FriendRequestNotFoundException(requestId));

        if (!request.getUser().getId().equals(currentUser.getId())) {
            throw new FriendRequestAccessDeniedException();
        }

        if (request.getStatus() != FriendListStatus.PENDING) {
            throw new InvalidFriendRequestStatusException(request.getStatus().name());
        }

        friendListRepository.delete(request);
        log.info("Friend request cancelled: id={}", requestId);
    }

    @Transactional
    public void removeFriend(Long friendId) {
        User currentUser = userService.getCurrentUser();

        if (currentUser.getId().equals(friendId)) {
            throw new CannotAddYourselfException();
        }

        FriendList friendship = friendListRepository
                .findBetweenUsers(currentUser.getId(), friendId)
                .orElseThrow(() -> new FriendNotFoundException(friendId));

        if (friendship.getStatus() != FriendListStatus.ACCEPTED) {
            throw new FriendNotFoundException(friendId);
        }

        friendListRepository.delete(friendship);
        log.info("Friend removed: user={}, friendId={}", currentUser.getUsername(), friendId);
    }

    private FriendResponse mapToFriendResponse(User user) {
        return FriendResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .build();
    }

    private FriendRequestResponse mapToRequestResponse(FriendList f) {
        return FriendRequestResponse.builder()
                .requestId(f.getId())
                .userId(f.getUser().getId())
                .username(f.getUser().getUsername())
                .createdAt(f.getCreatedAt())
                .build();
    }
}