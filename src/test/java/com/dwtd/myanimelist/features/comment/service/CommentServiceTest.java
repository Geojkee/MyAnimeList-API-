package com.dwtd.myanimelist.features.comment.service;

import com.dwtd.myanimelist.exception.anime.AnimeNotFoundException;
import com.dwtd.myanimelist.exception.comment.CommentAccessDeniedException;
import com.dwtd.myanimelist.exception.comment.CommentNotFoundException;
import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.anime.repository.AnimeRepository;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.auth.service.UserService;
import com.dwtd.myanimelist.features.comment.dto.CommentRequest;
import com.dwtd.myanimelist.features.comment.dto.CommentResponse;
import com.dwtd.myanimelist.features.comment.entity.Comment;
import com.dwtd.myanimelist.features.comment.repository.CommentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private AnimeRepository animeRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private CommentService commentService;

    private User testUser;
    private User adminUser;
    private User otherUser;
    private Anime testAnime;
    private Comment testComment;
    private CommentRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("TestUser")
                .role(Role.ROLE_USER)
                .build();

        adminUser = User.builder()
                .id(2L)
                .username("Admin")
                .role(Role.ROLE_ADMIN)
                .build();

        otherUser = User.builder()
                .id(3L)
                .username("OtherUser")
                .role(Role.ROLE_USER)
                .build();

        testAnime = Anime.builder()
                .id(1L)
                .titleRomaji("Naruto")
                .build();

        testComment = Comment.builder()
                .id(10L)
                .user(testUser)
                .anime(testAnime)
                .text("Great anime!")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testRequest = new CommentRequest("Great anime!");
    }

    @Test
    void create_shouldSaveCommentAndReturnResponse_whenValid() {
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(animeRepository.findById(1L)).thenReturn(Optional.of(testAnime));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        CommentResponse response = commentService.create(1L, testRequest);

        assertThat(response).isNotNull();
        assertThat(response.text()).isEqualTo("Great anime!");
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo("TestUser");
        assertThat(response.animeId()).isEqualTo(1L);

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getText()).isEqualTo("Great anime!");
        assertThat(captor.getValue().getUser()).isEqualTo(testUser);
        assertThat(captor.getValue().getAnime()).isEqualTo(testAnime);
    }

    @Test
    void create_shouldThrowAnimeNotFoundException_whenAnimeNotExists() {
        when(userService.getCurrentUser()).thenReturn(testUser);
        when(animeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(99L, testRequest))
                .isInstanceOf(AnimeNotFoundException.class)
                .hasMessageContaining("Anime with id 99 not found");

        verify(commentRepository, never()).save(any());
    }

    @Test
    void getByAnime_shouldReturnPageOfComments_whenAnimeExists() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Comment> page = new PageImpl<>(List.of(testComment));

        when(animeRepository.existsById(1L)).thenReturn(true);
        when(commentRepository.findByAnimeId(1L, pageable)).thenReturn(page);

        Page<CommentResponse> result = commentService.getByAnime(1L, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).text()).isEqualTo("Great anime!");
        assertThat(result.getContent().get(0).userId()).isEqualTo(1L);
        assertThat(result.getContent().get(0).username()).isEqualTo("TestUser");

        verify(animeRepository).existsById(1L);
        verify(commentRepository).findByAnimeId(1L, pageable);
    }

    @Test
    void getByAnime_shouldThrowAnimeNotFoundException_whenAnimeNotExists() {
        when(animeRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.getByAnime(99L, PageRequest.of(0, 10)))
                .isInstanceOf(AnimeNotFoundException.class)
                .hasMessageContaining("Anime with id 99 not found");

        verify(commentRepository, never()).findByAnimeId(any(), any());
    }

    @Test
    void delete_shouldDeleteComment_whenAuthor() {
        when(commentRepository.findById(10L)).thenReturn(Optional.of(testComment));
        when(userService.getCurrentUser()).thenReturn(testUser);

        commentService.delete(10L);

        verify(commentRepository).delete(testComment);
        verify(commentRepository).findById(10L);
    }

    @Test
    void delete_shouldDeleteComment_whenAdmin() {
        when(commentRepository.findById(10L)).thenReturn(Optional.of(testComment));
        when(userService.getCurrentUser()).thenReturn(adminUser);

        commentService.delete(10L);

        verify(commentRepository).delete(testComment);
        verify(commentRepository).findById(10L);
    }

    @Test
    void delete_shouldThrowCommentNotFoundException_whenCommentNotExists() {
        when(commentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.delete(999L))
                .isInstanceOf(CommentNotFoundException.class)
                .hasMessageContaining("Comment with id 999 not found");

        verify(commentRepository, never()).delete(any());
    }

    @Test
    void delete_shouldThrowCommentAccessDeniedException_whenNotAuthorAndNotAdmin() {
        when(commentRepository.findById(10L)).thenReturn(Optional.of(testComment));
        when(userService.getCurrentUser()).thenReturn(otherUser); // не автор и не админ

        assertThatThrownBy(() -> commentService.delete(10L))
                .isInstanceOf(CommentAccessDeniedException.class)
                .hasMessageContaining("You do not have permission");

        verify(commentRepository, never()).delete(any());
    }
}