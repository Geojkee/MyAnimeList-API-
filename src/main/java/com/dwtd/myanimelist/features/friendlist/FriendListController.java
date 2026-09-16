package com.dwtd.myanimelist.features.friendlist;

import com.dwtd.myanimelist.features.friendlist.dto.FriendRequestResponse;
import com.dwtd.myanimelist.features.friendlist.dto.FriendResponse;
import com.dwtd.myanimelist.features.friendlist.service.FriendListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/friends")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Friend List")
public class FriendListController {

    private final FriendListService friendListService;

    @Operation(summary = "Send friend request")
    @PostMapping("/{friendId}")
    public ResponseEntity<FriendRequestResponse> sendFriendRequest(@PathVariable Long friendId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(friendListService.sendFriendRequest(friendId));
    }

    @Operation(summary = "Get list of friends")
    @GetMapping
    public ResponseEntity<List<FriendResponse>> getFriends() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(friendListService.getFriends());
    }

    @Operation(summary = "Get incoming friend requests")
    @GetMapping("/requests/incoming")
    public ResponseEntity<List<FriendRequestResponse>> getIncomingRequests() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(friendListService.getIncomingRequests());
    }

    @Operation(summary = "Get outgoing friend requests")
    @GetMapping("/requests/outgoing")
    public ResponseEntity<List<FriendRequestResponse>> getOutgoingRequests() {
        return ResponseEntity.status(HttpStatus.OK)
                .body(friendListService.getOutgoingRequests());
    }

    @Operation(summary = "Accept friend request")
    @PatchMapping("/requests/{requestId}/accept")
    public ResponseEntity<FriendResponse> acceptRequest(@PathVariable Long requestId) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(friendListService.acceptFriendRequest(requestId));
    }

    @Operation(summary = "Reject friend request")
    @PatchMapping("/requests/{requestId}/reject")
    public ResponseEntity<Void> rejectRequest(@PathVariable Long requestId) {
        friendListService.rejectFriendRequest(requestId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Cancel outgoing friend request")
    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<Void> cancelRequest(@PathVariable Long requestId) {
        friendListService.cancelFriendRequest(requestId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Remove from friends")
    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> removeFriend(@PathVariable Long friendId) {
        friendListService.removeFriend(friendId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}