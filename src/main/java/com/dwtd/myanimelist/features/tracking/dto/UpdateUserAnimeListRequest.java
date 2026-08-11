package com.dwtd.myanimelist.features.tracking.dto;

import com.dwtd.myanimelist.features.tracking.enums.UserAnimeStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateUserAnimeListRequest(

        UserAnimeStatus status,

        @Min(value = 1, message = "Score must be between 1 and 10")
        @Max(value = 10, message = "Score must be between 1 and 10")
        Integer score,

        @Min(value = 0, message = "Watched episodes cannot be negative")
        Integer watchedEpisodes
) {
}