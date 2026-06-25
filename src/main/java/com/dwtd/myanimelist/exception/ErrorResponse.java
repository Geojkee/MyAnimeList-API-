package com.dwtd.myanimelist.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {


    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private final Instant timestamp;

    private final int status;
    private final String error;
    private final String message;
    private final String path;
    private final String errorCode;
    private final List<ValidationError> details;

    @Getter
    @Builder
    public static class ValidationError {
        private final String field;
        private final String message;
    }
}
