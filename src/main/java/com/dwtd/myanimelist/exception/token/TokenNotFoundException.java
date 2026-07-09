package com.dwtd.myanimelist.exception.token;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class TokenNotFoundException extends CustomException {
    public TokenNotFoundException() {
        super("Refresh token is expired or revoked",
                HttpStatus.NOT_FOUND, "TOKEN_NOT_FOUND");
    }
}
