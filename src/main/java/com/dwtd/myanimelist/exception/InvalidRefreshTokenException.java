package com.dwtd.myanimelist.exception;

import org.springframework.http.HttpStatus;

public class InvalidRefreshTokenException extends CustomException {
    public InvalidRefreshTokenException() {
        super("Refresh token is expired or revoked",
                HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN");
    }
}
