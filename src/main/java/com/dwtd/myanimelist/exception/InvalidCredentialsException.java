package com.dwtd.myanimelist.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends CustomException {
    public InvalidCredentialsException(String username) {
      super("Login failed: invalid credentials for " + username,
              HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}
