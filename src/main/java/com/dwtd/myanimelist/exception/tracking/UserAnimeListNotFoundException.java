package com.dwtd.myanimelist.exception.tracking;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UserAnimeListNotFoundException extends CustomException {
    public UserAnimeListNotFoundException(Long animeId) {
        super("Anime with id " + animeId + " not found in user's list",
                HttpStatus.NOT_FOUND,
                "USER_ANIME_LIST_NOT_FOUND");
    }
}