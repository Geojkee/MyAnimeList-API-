package com.dwtd.myanimelist.features.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "Comment text is required")
        @Size(max = 500, message = "Message must not exceed 500 characters")
        String text
) {
}