package com.dwtd.myanimelist.features.comment.service;

import com.dwtd.myanimelist.exception.anime.AnimeNotFoundException;
import com.dwtd.myanimelist.exception.comment.CommentAccessDeniedException;
import com.dwtd.myanimelist.exception.comment.CommentNotFoundException;
import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.anime.repository.AnimeRepository;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.service.UserService;
import com.dwtd.myanimelist.features.comment.dto.CommentRequest;
import com.dwtd.myanimelist.features.comment.dto.CommentResponse;
import com.dwtd.myanimelist.features.comment.entity.Comment;
import com.dwtd.myanimelist.features.comment.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final AnimeRepository animeRepository;
    private final UserService userService;

    @Transactional
    public CommentResponse create(Long animeId, CommentRequest request){
        User user = userService.getCurrentUser();

        Anime anime = animeRepository.findById(animeId)
                .orElseThrow(() -> new AnimeNotFoundException(animeId));

        Comment comment = Comment.builder()
                .user(user)
                .anime(anime)
                .text(request.text())
                .build();

        Comment saved = commentRepository.save(comment);
        log.info("Comment created: id={}, user={}, anime={}", saved.getId(), user.getUsername(), animeId);
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<CommentResponse> getByAnime(Long animeId, Pageable pageable) {
        if (!animeRepository.existsById(animeId)){
            throw new AnimeNotFoundException(animeId);
        }
        return commentRepository.findByAnimeId(animeId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public void delete(Long commentId){
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));

        User currentUser = userService.getCurrentUser();

        boolean isAdmin = currentUser.getRole().name().equals("ROLE_ADMIN");
        if (!comment.getUser().getId().equals(currentUser.getId()) && !isAdmin) {
            throw new CommentAccessDeniedException();
        }

        commentRepository.delete(comment);
        log.info("Comment deleted: id={}", commentId);
    }

    private CommentResponse mapToResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .userId(comment.getUser().getId())
                .username(comment.getUser().getUsername())
                .animeId(comment.getAnime().getId())
                .text(comment.getText())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
