package com.dwtd.myanimelist.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyExistsException extends CustomException {
    public EmailAlreadyExistsException(String email) {
        super("User with email " + email + " already exists",
                HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS");
    }
}
