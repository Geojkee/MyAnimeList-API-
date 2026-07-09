package com.dwtd.myanimelist.exception.user;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends CustomException {
    public UserNotFoundException(String username) {
        super("User not found: " + username,
                HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
    }
}
