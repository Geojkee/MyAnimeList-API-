package com.dwtd.myanimelist.exception.genre;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class GenreExistsException extends CustomException {
    public GenreExistsException(String name) {
        super("Genre with name: " + name + "already exists",
                HttpStatus.CONFLICT, "GENRE_ALREADY_EXISTS");
    }
}
