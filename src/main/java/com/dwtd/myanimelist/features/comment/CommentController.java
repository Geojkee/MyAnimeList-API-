package com.dwtd.myanimelist.features.comment;

import com.dwtd.myanimelist.features.comment.dto.CommentRequest;
import com.dwtd.myanimelist.features.comment.dto.CommentResponse;
import com.dwtd.myanimelist.features.comment.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/anime/{animeId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentResponse> create(
            @PathVariable Long animeId,
            @RequestBody @Valid CommentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.create(animeId, request));
    }

    @GetMapping
    public ResponseEntity<Page<CommentResponse>> get(
            @PathVariable Long animeId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(commentService.getByAnime(animeId, pageable));
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable Long commentId
    ) {
        commentService.delete(commentId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}