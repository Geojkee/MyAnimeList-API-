package com.dwtd.myanimelist.features.tracking.dto;

import com.dwtd.myanimelist.features.tracking.enums.UserAnimeStatus;
import lombok.Builder;

@Builder
public record UserAnimeListSummary(
        Long animeId,
        String titleRomaji,
        String titleEnglish,
        String imageUrl,
        UserAnimeStatus status,
        Integer score,
        Integer watchedEpisodes,
        Integer totalEpisodes
) {}