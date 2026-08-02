package com.dwtd.myanimelist.features.anime.service;

import com.dwtd.myanimelist.exception.anime.AnimeExistsException;
import com.dwtd.myanimelist.exception.anime.AnimeNotFoundException;
import com.dwtd.myanimelist.exception.genre.InvalidGenreIdsException;
import com.dwtd.myanimelist.features.anime.dto.AnimeResponse;
import com.dwtd.myanimelist.features.anime.dto.CreateAnimeRequest;
import com.dwtd.myanimelist.features.anime.dto.UpdateAnimeRequest;
import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.anime.enums.AnimeStatus;
import com.dwtd.myanimelist.features.anime.enums.AnimeType;
import com.dwtd.myanimelist.features.anime.repository.AnimeRepository;
import com.dwtd.myanimelist.features.genre.entity.Genre;
import com.dwtd.myanimelist.features.genre.repository.GenreRepository;
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
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnimeServiceTest {

    @Mock
    private AnimeRepository animeRepository;

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private AnimeService animeService;

    private final Instant now = Instant.now();

    private Anime testAnime;
    private CreateAnimeRequest createAnimeRequest;
    private UpdateAnimeRequest updateAnimeRequest;

    @BeforeEach
    void setUp() {
        testAnime = Anime.builder()
                .id(1L)
                .titleRomaji("Naruto")
                .titleEnglish("Naruto")
                .type(AnimeType.TV)
                .episodeCount(220)
                .status(AnimeStatus.FINISHED)
                .synopsis("Test synopsis")
                .createdAt(now)
                .updatedAt(now)
                .build();

        createAnimeRequest = CreateAnimeRequest.builder()
                .titleRomaji("Naruto")
                .titleEnglish("Naruto")
                .type(AnimeType.TV)
                .episodeCount(220)
                .status(AnimeStatus.FINISHED)
                .synopsis("Test synopsis")
                .build();

        updateAnimeRequest = UpdateAnimeRequest.builder()
                .titleRomaji("Naruto")
                .titleEnglish("Naruto")
                .type(AnimeType.TV)
                .episodeCount(220)
                .status(AnimeStatus.FINISHED)
                .synopsis("Test synopsis")
                .build();
    }

    @Test
    void findAll_shouldReturnPageOfAnimeResponses() {
        Specification<Anime> spec = mock(Specification.class);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Anime> page = new PageImpl<>(List.of(testAnime));

        when(animeRepository.findAll(spec, pageable)).thenReturn(page);

        Page<AnimeResponse> result = animeService.findAll(spec, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).titleRomaji()).isEqualTo("Naruto");
        verify(animeRepository).findAll(spec, pageable);
    }

    @Test
    void findById_shouldReturnAnimeResponse_whenExists() {
        when(animeRepository.findById(1L)).thenReturn(Optional.of(testAnime));

        AnimeResponse response = animeService.findById(1L);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.titleRomaji()).isEqualTo("Naruto");
        assertThat(response.type()).isEqualTo(AnimeType.TV);
        assertThat(response.episodeCount()).isEqualTo(220);
        assertThat(response.status()).isEqualTo(AnimeStatus.FINISHED);
        verify(animeRepository).findById(1L);
    }

    @Test
    void findById_shouldThrowAnimeNotFoundException_whenNotFound() {
        when(animeRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> animeService.findById(999L))
                .isInstanceOf(AnimeNotFoundException.class)
                .hasMessageContaining("Anime with id 999 not found");
        verify(animeRepository).findById(999L);
    }

    @Test
    void create_shouldSaveAnimeAndReturnResponse_whenNoDuplicate() {
        when(animeRepository.existsByTitleRomaji("Naruto")).thenReturn(false);
        when(animeRepository.save(any(Anime.class))).thenReturn(testAnime);

        AnimeResponse response = animeService.create(createAnimeRequest);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.titleRomaji()).isEqualTo("Naruto");

        ArgumentCaptor<Anime> animeCaptor = ArgumentCaptor.forClass(Anime.class);
        verify(animeRepository).save(animeCaptor.capture());
        Anime saved = animeCaptor.getValue();
        assertThat(saved.getTitleRomaji()).isEqualTo("Naruto");
        assertThat(saved.getTitleEnglish()).isEqualTo("Naruto");
        assertThat(saved.getType()).isEqualTo(AnimeType.TV);
        assertThat(saved.getEpisodeCount()).isEqualTo(220);
        assertThat(saved.getStatus()).isEqualTo(AnimeStatus.FINISHED);
    }

    @Test
    void create_shouldThrowAnimeExistsException_whenDuplicateTitle() {
        when(animeRepository.existsByTitleRomaji("Naruto")).thenReturn(true);

        assertThatThrownBy(() -> animeService.create(createAnimeRequest))
                .isInstanceOf(AnimeExistsException.class)
                .hasMessageContaining("Anime with title Naruto already exists");
        verify(animeRepository, never()).save(any());
    }

    @Test
    void create_shouldSaveAnimeWithGenres_whenGenreIdsProvided() {
        Set<Long> genreIds = Set.of(1L, 2L);
        CreateAnimeRequest requestWithGenres = CreateAnimeRequest.builder()
                .titleRomaji("Test Anime")
                .titleEnglish("Test Anime")
                .type(AnimeType.TV)
                .episodeCount(12)
                .status(AnimeStatus.FINISHED)
                .synopsis("Test")
                .genreIds(genreIds)
                .build();

        List<Genre> genres = List.of(
                Genre.builder().id(1L).name("Action").build(),
                Genre.builder().id(2L).name("Adventure").build()
        );

        when(animeRepository.existsByTitleRomaji("Test Anime")).thenReturn(false);
        when(genreRepository.findAllById(genreIds)).thenReturn(genres);
        when(animeRepository.save(any(Anime.class))).thenAnswer(invocation -> {
            Anime anime = invocation.getArgument(0);
            anime.setId(1L);
            return anime;
        });

        AnimeResponse response = animeService.create(requestWithGenres);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.genres()).hasSize(2);
        assertThat(response.genres()).extracting("id").containsExactlyInAnyOrder(1L, 2L);
        verify(genreRepository).findAllById(genreIds);
    }

    @Test
    void create_shouldThrowInvalidGenreIdsException_whenSomeGenresNotFound() {
        Set<Long> genreIds = Set.of(1L, 99L);
        CreateAnimeRequest requestWithGenres = CreateAnimeRequest.builder()
                .titleRomaji("Test Anime")
                .type(AnimeType.TV)
                .episodeCount(12)
                .status(AnimeStatus.FINISHED)
                .genreIds(genreIds)
                .build();

        when(animeRepository.existsByTitleRomaji("Test Anime")).thenReturn(false);
        when(genreRepository.findAllById(genreIds)).thenReturn(List.of(Genre.builder().id(1L).build())); // только один найден

        assertThatThrownBy(() -> animeService.create(requestWithGenres))
                .isInstanceOf(InvalidGenreIdsException.class)
                .hasMessageContaining("Some genres not found");
        verify(animeRepository, never()).save(any());
    }

    @Test
    void update_shouldUpdateAndReturnResponse_whenValid() {
        Long id = 1L;
        UpdateAnimeRequest updateRequest = UpdateAnimeRequest.builder()
                .titleRomaji("Naruto Shippuden")
                .titleEnglish("Naruto Shippuden")
                .type(AnimeType.TV)
                .episodeCount(500)
                .status(AnimeStatus.FINISHED)
                .synopsis("Updated synopsis")
                .build();

        Anime existing = testAnime;

        when(animeRepository.findById(id)).thenReturn(Optional.of(existing));
        when(animeRepository.existsByTitleRomajiAndIdNot(updateRequest.titleRomaji(), id)).thenReturn(false);
        when(animeRepository.save(any(Anime.class))).thenAnswer(invocation -> {
            Anime saved = invocation.getArgument(0);
            saved.setId(id);
            return saved;
        });

        AnimeResponse response = animeService.update(id, updateRequest);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(id);
        assertThat(response.titleRomaji()).isEqualTo("Naruto Shippuden");
        assertThat(response.episodeCount()).isEqualTo(500);

        ArgumentCaptor<Anime> animeCaptor = ArgumentCaptor.forClass(Anime.class);
        verify(animeRepository).save(animeCaptor.capture());
        Anime captured = animeCaptor.getValue();
        assertThat(captured.getTitleRomaji()).isEqualTo("Naruto Shippuden");
        assertThat(captured.getEpisodeCount()).isEqualTo(500);
        verify(animeRepository).findById(id);
    }

    @Test
    void update_shouldThrowAnimeNotFoundException_whenNotFound() {
        Long id = 999L;
        when(animeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> animeService.update(id, updateAnimeRequest))
                .isInstanceOf(AnimeNotFoundException.class)
                .hasMessageContaining("Anime with id 999 not found");
        verify(animeRepository, never()).save(any());
    }

    @Test
    void update_shouldThrowAnimeExistsException_whenDuplicateTitle() {
        Long id = 1L;
        Anime existing = testAnime;
        when(animeRepository.findById(id)).thenReturn(Optional.of(existing));
        when(animeRepository.existsByTitleRomajiAndIdNot("Naruto", id)).thenReturn(true);

        assertThatThrownBy(() -> animeService.update(id, updateAnimeRequest))
                .isInstanceOf(AnimeExistsException.class)
                .hasMessageContaining("Anime with title Naruto already exists");
        verify(animeRepository, never()).save(any());
    }

    @Test
    void update_shouldUpdateGenres_whenGenreIdsProvided() {
        Long id = 1L;
        Anime existing = testAnime; // без жанров
        Set<Long> genreIds = Set.of(1L, 2L);
        UpdateAnimeRequest updateRequest = UpdateAnimeRequest.builder()
                .titleRomaji("Naruto")
                .type(AnimeType.TV)
                .episodeCount(220)
                .status(AnimeStatus.FINISHED)
                .genreIds(genreIds)
                .build();

        List<Genre> genres = List.of(
                Genre.builder().id(1L).name("Action").build(),
                Genre.builder().id(2L).name("Adventure").build()
        );

        when(animeRepository.findById(id)).thenReturn(Optional.of(existing));
        when(animeRepository.existsByTitleRomajiAndIdNot(updateRequest.titleRomaji(), id)).thenReturn(false);
        when(genreRepository.findAllById(genreIds)).thenReturn(genres);
        when(animeRepository.save(any(Anime.class))).thenReturn(existing);

        AnimeResponse response = animeService.update(id, updateRequest);

        assertThat(response.genres()).hasSize(2);
        assertThat(response.genres()).extracting("id").containsExactlyInAnyOrder(1L, 2L);
        verify(genreRepository).findAllById(genreIds);
        verify(animeRepository).save(existing);
    }

    @Test
    void update_shouldClearGenres_whenGenreIdsIsEmpty() {
        Long id = 1L;
        Anime existing = testAnime;
        existing.setGenres(Set.of(Genre.builder().id(1L).name("Action").build()));

        UpdateAnimeRequest updateRequest = UpdateAnimeRequest.builder()
                .titleRomaji("Naruto")
                .type(AnimeType.TV)
                .episodeCount(220)
                .status(AnimeStatus.FINISHED)
                .genreIds(Set.of()) // пустой список
                .build();

        when(animeRepository.findById(id)).thenReturn(Optional.of(existing));
        when(animeRepository.existsByTitleRomajiAndIdNot(updateRequest.titleRomaji(), id)).thenReturn(false);
        when(animeRepository.save(any(Anime.class))).thenReturn(existing);

        AnimeResponse response = animeService.update(id, updateRequest);

        assertThat(response.genres()).isEmpty();
        verify(genreRepository, never()).findAllById(any());
        verify(animeRepository).save(existing);
    }

    @Test
    void update_shouldThrowInvalidGenreIdsException_whenSomeGenresNotFound() {
        Long id = 1L;
        Anime existing = testAnime;
        Set<Long> genreIds = Set.of(1L, 99L);
        UpdateAnimeRequest updateRequest = UpdateAnimeRequest.builder()
                .titleRomaji("Naruto")
                .type(AnimeType.TV)
                .episodeCount(220)
                .status(AnimeStatus.FINISHED)
                .genreIds(genreIds)
                .build();

        when(animeRepository.findById(id)).thenReturn(Optional.of(existing));
        when(animeRepository.existsByTitleRomajiAndIdNot(updateRequest.titleRomaji(), id)).thenReturn(false);
        when(genreRepository.findAllById(genreIds)).thenReturn(List.of(Genre.builder().id(1L).build()));

        assertThatThrownBy(() -> animeService.update(id, updateRequest))
                .isInstanceOf(InvalidGenreIdsException.class)
                .hasMessageContaining("Some genres not found");
        verify(animeRepository, never()).save(any());
    }

    @Test
    void delete_shouldDelete_whenExists() {
        Long id = 1L;
        when(animeRepository.existsById(id)).thenReturn(true);

        animeService.delete(id);

        verify(animeRepository).existsById(id);
        verify(animeRepository).deleteById(id);
    }

    @Test
    void delete_shouldThrowAnimeNotFoundException_whenNotFound() {
        Long id = 999L;
        when(animeRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> animeService.delete(id))
                .isInstanceOf(AnimeNotFoundException.class)
                .hasMessageContaining("Anime with id 999 not found");
        verify(animeRepository, never()).deleteById(anyLong());
    }
}