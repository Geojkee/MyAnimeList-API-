package com.dwtd.myanimelist.exception.tracking;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UserAnimeListAlreadyExistsException extends CustomException {
    public UserAnimeListAlreadyExistsException(Long animeId) {
        super("Anime with id " + animeId + " already exists in user's list",
                HttpStatus.CONFLICT,
                "ANIME_ALREADY_EXISTS_IN_USER_LIST");
    }
}
