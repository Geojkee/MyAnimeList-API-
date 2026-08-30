package com.dwtd.myanimelist.exception.comment;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class CommentNotFoundException extends CustomException {
    public CommentNotFoundException(Long id) {
        super("Comment with id " + id + " not found",
                HttpStatus.NOT_FOUND,
                "COMMENT_NOT_FOUND");
    }
}
