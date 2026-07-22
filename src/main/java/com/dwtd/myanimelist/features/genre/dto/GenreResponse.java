package com.dwtd.myanimelist.features.genre.dto;

import lombok.Builder;

@Builder
public record GenreResponse(
        Long id,
        String name
) {
}
