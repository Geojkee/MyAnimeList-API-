package com.dwtd.myanimelist.features.friendlist.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record FriendRequestResponse(
        Long requestId,
        Long userId,
        String username,
        Instant createdAt
) {
}
