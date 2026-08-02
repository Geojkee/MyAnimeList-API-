package com.dwtd.myanimelist.exception.genre;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class GenreNotFoundException extends CustomException {
    public GenreNotFoundException(Long id) {
        super("Genre with id " + id + " not found",
                HttpStatus.NOT_FOUND, "GENRE_NOT_FOUND");
    }
}
