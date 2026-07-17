package com.dwtd.myanimelist.features.anime.dto;

import com.dwtd.myanimelist.features.anime.enums.AnimeStatus;
import com.dwtd.myanimelist.features.anime.enums.AnimeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;


public record AnimeRequest(
        @NotBlank(message = "Title (romaji) is required")
        String titleRomaji,

        String titleEnglish,

        @NotNull(message = "Type is required")
        AnimeType type,

        @Min(value = 0, message = "Episode count must be at least 0")
        Integer episodeCount,

        @NotNull(message = "Status is required")
        AnimeStatus status,

        String synopsis
) {
}