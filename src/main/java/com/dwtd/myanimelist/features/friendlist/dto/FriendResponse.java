package com.dwtd.myanimelist.features.friendlist.dto;

import lombok.Builder;

@Builder
public record FriendResponse(
        Long userId,
        String username
) {
}
