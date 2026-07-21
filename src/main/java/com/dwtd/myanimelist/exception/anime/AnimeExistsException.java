package com.dwtd.myanimelist.exception.anime;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class AnimeExistsException extends CustomException {
    public AnimeExistsException(String titleRomaji) {
        super("Anime with title '" + titleRomaji + "' already exists",
                HttpStatus.CONFLICT,
                "ANIME_ALREADY_EXISTS");
    }
}
