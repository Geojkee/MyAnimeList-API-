package com.dwtd.myanimelist.exception.friendlist;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class CannotAddYourselfException extends CustomException {
    public CannotAddYourselfException() {
        super("You cannot add yourself as a friend",
                HttpStatus.BAD_REQUEST,
                "CANNOT_ADD_YOURSELF");
    }
}