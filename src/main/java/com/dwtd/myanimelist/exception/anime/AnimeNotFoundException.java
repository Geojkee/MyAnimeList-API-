package com.dwtd.myanimelist.exception.anime;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class AnimeNotFoundException extends CustomException {
    public AnimeNotFoundException(Long id) {
        super("Anime with id " + id + " not found",
                HttpStatus.NOT_FOUND, "ANINE_NOT_FOUND");
    }
}