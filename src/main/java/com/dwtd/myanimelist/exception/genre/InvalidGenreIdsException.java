package com.dwtd.myanimelist.exception.genre;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidGenreIdsException extends CustomException {
    public InvalidGenreIdsException() {
        super("Some genres not found",
                HttpStatus.BAD_REQUEST, "INVALID_GENRE_IDS");
    }
}
