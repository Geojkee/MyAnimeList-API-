package com.dwtd.myanimelist.features.anime.dto;

import com.dwtd.myanimelist.features.anime.enums.AnimeStatus;
import com.dwtd.myanimelist.features.anime.enums.AnimeType;
import com.dwtd.myanimelist.features.genre.dto.GenreResponse;
import lombok.Builder;

import java.time.Instant;
import java.util.Set;

@Builder
public record AnimeResponse(
        Long id,
        String titleRomaji,
        String titleEnglish,
        Set<GenreResponse> genres,
        AnimeType type,
        Integer episodeCount,
        AnimeStatus status,
        String synopsis,
        Instant createdAt,
        Instant updatedAt
) {
}