package com.dwtd.myanimelist.features.genre.service;

import com.dwtd.myanimelist.exception.genre.GenreExistsException;
import com.dwtd.myanimelist.exception.genre.GenreNotFoundException;
import com.dwtd.myanimelist.features.genre.dto.GenreRequest;
import com.dwtd.myanimelist.features.genre.dto.GenreResponse;
import com.dwtd.myanimelist.features.genre.entity.Genre;
import com.dwtd.myanimelist.features.genre.repository.GenreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenreServiceTest {

    @Mock
    private GenreRepository genreRepository;

    @InjectMocks
    private GenreService genreService;

    @Test
    void getAll_shouldReturnListOfGenreResponses() {
        Genre genre1 = Genre.builder().id(1L).name("Action").build();
        Genre genre2 = Genre.builder().id(2L).name("Adventure").build();
        when(genreRepository.findAll()).thenReturn(List.of(genre1, genre2));

        List<GenreResponse> result = genreService.getAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Action");
        assertThat(result.get(1).name()).isEqualTo("Adventure");
        verify(genreRepository).findAll();
    }

    @Test
    void findById_shouldReturnGenreResponse_whenExists() {
        Long id = 1L;
        Genre genre = Genre.builder().id(id).name("Action").build();
        when(genreRepository.findById(id)).thenReturn(Optional.of(genre));

        GenreResponse response = genreService.findById(id);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.name()).isEqualTo("Action");
        verify(genreRepository).findById(id);
    }

    @Test
    void findById_shouldThrowGenreNotFoundException_whenNotExists() {
        Long id = 999L;
        when(genreRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> genreService.findById(id))
                .isInstanceOf(GenreNotFoundException.class)
                .hasMessageContaining("Genre with id 999 not found");
        verify(genreRepository).findById(id);
    }

    @Test
    void create_shouldSaveAndReturnResponse_whenNameIsFree() {
        GenreRequest request = GenreRequest.builder().name("Action").build();
        when(genreRepository.existsByName(request.name())).thenReturn(false);

        Genre savedGenre = Genre.builder().id(1L).name(request.name()).build();
        when(genreRepository.save(any(Genre.class))).thenReturn(savedGenre);

        GenreResponse response = genreService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Action");

        ArgumentCaptor<Genre> captor = ArgumentCaptor.forClass(Genre.class);
        verify(genreRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo(request.name());
    }

    @Test
    void create_shouldThrowGenreExistsException_whenNameAlreadyExists() {
        GenreRequest request = GenreRequest.builder().name("Action").build();
        when(genreRepository.existsByName(request.name())).thenReturn(true);

        assertThatThrownBy(() -> genreService.create(request))
                .isInstanceOf(GenreExistsException.class)
                .hasMessageContaining("Genre with name: Action already exists");
        verify(genreRepository, never()).save(any());
    }


    @Test
    void update_shouldUpdateAndReturnResponse_whenNameIsFree() {
        Long id = 1L;
        GenreRequest request = GenreRequest.builder().name("Adventure").build();
        Genre existing = Genre.builder().id(id).name("Action").build();

        when(genreRepository.findById(id)).thenReturn(Optional.of(existing));
        when(genreRepository.existsByName(request.name())).thenReturn(false);

        Genre updated = Genre.builder().id(id).name(request.name()).build();
        when(genreRepository.save(existing)).thenReturn(updated);

        GenreResponse response = genreService.update(id, request);

        assertThat(response.name()).isEqualTo(request.name());
        verify(genreRepository).existsByName(request.name());
        verify(genreRepository).save(existing);
    }

    @Test
    void update_shouldSkipDuplicateCheck_whenNameIsSame() {
        Long id = 1L;
        String sameName = "Action";
        GenreRequest request = GenreRequest.builder().name(sameName).build();
        Genre existing = Genre.builder().id(id).name(sameName).build();

        when(genreRepository.findById(id)).thenReturn(Optional.of(existing));
        when(genreRepository.save(existing)).thenReturn(existing);

        GenreResponse response = genreService.update(id, request);

        assertThat(response.name()).isEqualTo(sameName);
        verify(genreRepository, never()).existsByName(any());
        verify(genreRepository).save(existing);
    }

    @Test
    void update_shouldThrowGenreExistsException_whenNameAlreadyTakenByOther() {
        Long id = 1L;
        GenreRequest request = GenreRequest.builder().name("Adventure").build();
        Genre existing = Genre.builder().id(id).name("Action").build();

        when(genreRepository.findById(id)).thenReturn(Optional.of(existing));
        when(genreRepository.existsByName(request.name())).thenReturn(true);

        assertThatThrownBy(() -> genreService.update(id, request))
                .isInstanceOf(GenreExistsException.class)
                .hasMessageContaining("Genre with name: Adventure already exists");
        verify(genreRepository, never()).save(any());
    }

    @Test
    void update_shouldThrowGenreNotFoundException_whenNotExists() {
        Long id = 999L;
        GenreRequest request = GenreRequest.builder().name("Any").build();
        when(genreRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> genreService.update(id, request))
                .isInstanceOf(GenreNotFoundException.class);
        verify(genreRepository, never()).save(any());
    }

    @Test
    void delete_shouldDelete_whenExists() {
        Long id = 1L;
        Genre genre = Genre.builder().id(id).name("Action").build();
        when(genreRepository.findById(id)).thenReturn(Optional.of(genre));

        genreService.delete(id);

        verify(genreRepository).delete(genre);
    }

    @Test
    void delete_shouldThrowGenreNotFoundException_whenNotExists() {
        Long id = 999L;
        when(genreRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> genreService.delete(id))
                .isInstanceOf(GenreNotFoundException.class);
        verify(genreRepository, never()).delete(any());
    }
}