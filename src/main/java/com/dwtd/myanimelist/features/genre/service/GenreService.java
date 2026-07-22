package com.dwtd.myanimelist.features.genre.service;

import com.dwtd.myanimelist.exception.genre.GenreExistsException;
import com.dwtd.myanimelist.exception.genre.GenreNotFoundException;
import com.dwtd.myanimelist.features.genre.dto.GenreRequest;
import com.dwtd.myanimelist.features.genre.dto.GenreResponse;
import com.dwtd.myanimelist.features.genre.entity.Genre;
import com.dwtd.myanimelist.features.genre.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenreService {

    private final GenreRepository genreRepository;

    @Transactional
    public GenreResponse create(GenreRequest request) {
        if (genreRepository.existsByName(request.name())) {
            throw new GenreExistsException(request.name());
        }

        Genre genre = Genre.builder()
                .name(request.name())
                .build();

        Genre savedGenre = genreRepository.save(genre);
        log.info("Genre created: id={}, name={}", savedGenre.getId(), savedGenre.getName());
        return mapToResponse(savedGenre);
    }

    @Transactional(readOnly = true)
    public List<GenreResponse> getAll(){
        return genreRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public GenreResponse findById(Long id){
        Genre genre = getGenre(id);

        return mapToResponse(genre);
    }

    @Transactional
    public GenreResponse update(Long id, GenreRequest request){
        Genre genre = getGenre(id);

        if (!genre.getName().equals(request.name()) && genreRepository.existsByName(request.name())) {
            throw new GenreExistsException(request.name());
        }

        genre.setName(request.name());

        Genre updateGenre = genreRepository.save(genre);
        log.info("Genre updated: id={}, name={}", updateGenre.getId(), updateGenre.getName());
        return mapToResponse(updateGenre);
    }

    @Transactional
    public void delete(Long id){
        Genre genre = getGenre(id);

        genreRepository.delete(genre);
        log.info("Genre deleted: id={}, name={}", id, genre.getName());
    }

    private GenreResponse mapToResponse(Genre genre) {
        return GenreResponse.builder()
                .id(genre.getId())
                .name(genre.getName())
                .build();
    }

    private Genre getGenre(Long id){
        return genreRepository.findById(id)
                .orElseThrow(() -> new GenreNotFoundException(id));
    }
}
