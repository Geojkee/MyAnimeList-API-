package com.dwtd.myanimelist.features.tracking.dto;

import com.dwtd.myanimelist.features.tracking.enums.UserAnimeStatus;
import lombok.Builder;

@Builder
public record UserAnimeListResponse(
        Long id,
        Long userId,
        Long animeId,
        String titleRomaji,
        String titleEnglish,
        UserAnimeStatus status,
        Integer score,
        Integer watchedEpisodes,
        Integer totalEpisodes,
        String synopsis
) {}