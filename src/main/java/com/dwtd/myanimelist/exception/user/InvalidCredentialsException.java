package com.dwtd.myanimelist.exception.user;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends CustomException {
    public InvalidCredentialsException(String username) {
      super("Login failed: invalid credentials for " + username,
              HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }
}
