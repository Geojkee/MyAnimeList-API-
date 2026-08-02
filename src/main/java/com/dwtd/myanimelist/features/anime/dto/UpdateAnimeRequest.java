package com.dwtd.myanimelist.features.anime.dto;

import com.dwtd.myanimelist.features.anime.enums.AnimeStatus;
import com.dwtd.myanimelist.features.anime.enums.AnimeType;
import jakarta.validation.constraints.Min;
import lombok.Builder;

import java.util.Set;

@Builder
public record UpdateAnimeRequest(
        String titleRomaji,

        String titleEnglish,

        AnimeType type,

        Set<Long> genreIds,

        @Min(value = 0, message = "Episode count must be at least 0")
        Integer episodeCount,

        AnimeStatus status,

        String synopsis
) {
}