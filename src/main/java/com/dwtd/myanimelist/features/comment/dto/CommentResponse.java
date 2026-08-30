package com.dwtd.myanimelist.features.comment.dto;

import lombok.Builder;

import java.time.Instant;

@Builder
public record CommentResponse(
        Long id,
        Long userId,
        String username,
        Long animeId,
        String text,
        Instant createdAt,
        Instant updatedAt
) {
}
