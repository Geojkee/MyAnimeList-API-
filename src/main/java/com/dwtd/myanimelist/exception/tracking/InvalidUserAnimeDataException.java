package com.dwtd.myanimelist.exception.tracking;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidUserAnimeDataException extends CustomException {
    public InvalidUserAnimeDataException(String message) {
        super(message,
                HttpStatus.BAD_REQUEST,
                "INVALID_USER_ANIME_DATA");
    }
}