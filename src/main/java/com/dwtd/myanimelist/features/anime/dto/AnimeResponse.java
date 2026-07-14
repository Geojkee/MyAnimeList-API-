package com.dwtd.myanimelist.features.anime.dto;

import com.dwtd.myanimelist.features.anime.enums.AnimeStatus;
import com.dwtd.myanimelist.features.anime.enums.AnimeType;
import lombok.Builder;

import java.time.Instant;

@Builder
public record AnimeResponse(
        Long id,
        String titleRomaji,
        String titleEnglish,
        AnimeType type,
        Integer episodeCount,
        AnimeStatus status,
        String synopsis,
        Instant createdAt,
        Instant updatedAt
) {
}
