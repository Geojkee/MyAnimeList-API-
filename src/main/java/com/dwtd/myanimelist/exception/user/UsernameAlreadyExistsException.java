package com.dwtd.myanimelist.exception.user;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class UsernameAlreadyExistsException extends CustomException {
    public UsernameAlreadyExistsException(String username) {
        super("User with username " + username + " already exists",
                HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS");
    }
}
