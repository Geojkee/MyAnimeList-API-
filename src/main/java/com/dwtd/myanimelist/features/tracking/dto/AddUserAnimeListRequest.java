package com.dwtd.myanimelist.features.tracking.dto;

import jakarta.validation.constraints.NotNull;

public record AddUserAnimeListRequest(
        @NotNull(message = "Anime ID is required")
        Long animeId
) {
}