package com.dwtd.myanimelist.exception;

import org.springframework.http.HttpStatus;

public class UsernameAlreadyExistsException extends CustomException {
    public UsernameAlreadyExistsException(String username) {
        super("User with username " + username + " already exists",
                HttpStatus.CONFLICT, "USERNAME_ALREADY_EXISTS");
    }
}
