package com.dwtd.myanimelist.exception.user;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UserNotAuthenticatedException extends CustomException {
    public UserNotAuthenticatedException() {
        super("Authentication required",
                HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}
