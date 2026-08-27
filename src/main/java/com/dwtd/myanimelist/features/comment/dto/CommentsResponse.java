package com.dwtd.myanimelist.features.comment.dto;

import java.time.Instant;

public record CommentsResponse(
        Long id,
        Long userId,
        String username,
        Long animeId,
        String text,
        Instant createdAt,
        Instant updatedAt
) {
}
