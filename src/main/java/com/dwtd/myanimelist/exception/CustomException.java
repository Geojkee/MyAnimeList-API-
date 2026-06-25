package com.dwtd.myanimelist.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class CustomException extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    protected CustomException(
            String message,
            HttpStatus status,
            String errorCode
    ) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}
