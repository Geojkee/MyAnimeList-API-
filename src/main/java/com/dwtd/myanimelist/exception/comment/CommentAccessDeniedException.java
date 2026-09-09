package com.dwtd.myanimelist.exception.comment;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class CommentAccessDeniedException extends CustomException {
    public CommentAccessDeniedException() {
        super("You do not have permission to modify this comment",
                HttpStatus.FORBIDDEN,
                "COMMENT_ACCESS_DENIED");
    }

}
