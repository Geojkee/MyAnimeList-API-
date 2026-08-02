package com.dwtd.myanimelist.features.anime.service;

import com.dwtd.myanimelist.exception.anime.AnimeExistsException;
import com.dwtd.myanimelist.exception.anime.AnimeNotFoundException;
import com.dwtd.myanimelist.exception.genre.InvalidGenreIdsException;
import com.dwtd.myanimelist.features.anime.dto.CreateAnimeRequest;
import com.dwtd.myanimelist.features.anime.dto.AnimeResponse;
import com.dwtd.myanimelist.features.anime.dto.UpdateAnimeRequest;
import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.anime.repository.AnimeRepository;
import com.dwtd.myanimelist.features.genre.dto.GenreResponse;
import com.dwtd.myanimelist.features.genre.entity.Genre;
import com.dwtd.myanimelist.features.genre.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnimeService {

    private final AnimeRepository animeRepository;

    private final GenreRepository genreRepository;

    @Transactional
    public AnimeResponse create(CreateAnimeRequest request) {
        if (animeRepository.existsByTitleRomaji(request.titleRomaji())) {
            throw new AnimeExistsException(request.titleRomaji());
        }

        Anime anime = Anime.builder()
                .titleRomaji(request.titleRomaji())
                .titleEnglish(request.titleEnglish())
                .type(request.type())
                .episodeCount(request.episodeCount())
                .status(request.status())
                .synopsis(request.synopsis())
                .build();

        anime.setGenres(fetchGenres(request.genreIds()));

        Anime savedAnime = animeRepository.save(anime);
        log.info("Anime created: id={}, title={}", savedAnime.getId(), savedAnime.getTitleRomaji());
        return mapToResponse(savedAnime);
    }

    @Transactional(readOnly = true)
    public Page<AnimeResponse> findAll(Specification<Anime> specification, Pageable pageable) {
        return animeRepository.findAll(specification, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public AnimeResponse findById(Long id) {
        Anime anime = animeRepository.findById(id)
                .orElseThrow(() -> new AnimeNotFoundException(id));

        return mapToResponse(anime);
    }

    @Transactional
    public AnimeResponse update(Long id, UpdateAnimeRequest request) {
        Anime anime = animeRepository.findById(id)
                .orElseThrow(() -> new AnimeNotFoundException(id));

        if (request.titleRomaji() != null){
            if (animeRepository.existsByTitleRomajiAndIdNot(request.titleRomaji(), id)) {
                throw new AnimeExistsException(request.titleRomaji());
            }
            anime.setTitleRomaji(request.titleRomaji());
        }

        if (request.titleEnglish() != null) {
            anime.setTitleEnglish(request.titleEnglish());
        }

        if (request.type() != null) {
            anime.setType(request.type());
        }

        if (request.episodeCount() != null) {
            anime.setEpisodeCount(request.episodeCount());
        }

        if (request.status() != null) {
            anime.setStatus(request.status());
        }

        if (request.synopsis() != null) {
            anime.setSynopsis(request.synopsis());
        }

        if (request.genreIds() != null){
            anime.setGenres(fetchGenres(request.genreIds()));
        }

        Anime updatedAnime = animeRepository.save(anime);
        log.info("Anime updated: id={}, title={}", updatedAnime.getId(), updatedAnime.getTitleRomaji());
        return mapToResponse(updatedAnime);
    }

    @Transactional
    public void delete(Long id) {
        if (!animeRepository.existsById(id)) {
            throw new AnimeNotFoundException(id);
        }
        animeRepository.deleteById(id);
        log.info("Anime deleted: id={}", id);
    }

    private AnimeResponse mapToResponse(Anime anime) {
        Set<GenreResponse> genreResponse = anime.getGenres() != null
                ? anime.getGenres().stream()
                .map(genre -> GenreResponse.builder()
                        .id(genre.getId())
                        .name(genre.getName())
                        .build())
                .collect(Collectors.toSet())
                : Set.of();

        return AnimeResponse.builder()
                .id(anime.getId())
                .titleRomaji(anime.getTitleRomaji())
                .titleEnglish(anime.getTitleEnglish())
                .type(anime.getType())
                .episodeCount(anime.getEpisodeCount())
                .status(anime.getStatus())
                .synopsis(anime.getSynopsis())
                .genres(genreResponse)
                .createdAt(anime.getCreatedAt())
                .updatedAt(anime.getUpdatedAt())
                .build();
    }

    private Set<Genre> fetchGenres(Set<Long> genreIds) {
        if (genreIds == null || genreIds.isEmpty()) {
            return Set.of();
        }
        Set<Genre> genres = new HashSet<>(genreRepository.findAllById(genreIds));
        if (genres.size() != genreIds.size()) {
            throw new InvalidGenreIdsException();
        }
        return genres;
    }
}