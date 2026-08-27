package com.dwtd.myanimelist.features.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "Comment test is required")
        @Size(max = 500, message = "Message must not exceed 500 charcters")
        String text
) {
}
