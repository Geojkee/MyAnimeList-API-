package com.dwtd.myanimelist.features.anime.service;

import com.dwtd.myanimelist.exception.anime.AnimeExistsException;
import com.dwtd.myanimelist.exception.anime.AnimeNotFoundException;
import com.dwtd.myanimelist.features.anime.dto.AnimeRequest;
import com.dwtd.myanimelist.features.anime.dto.AnimeResponse;
import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.anime.repository.AnimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnimeService {

    private final AnimeRepository animeRepository;

    @Transactional
    public AnimeResponse create(AnimeRequest request) {
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
    public AnimeResponse update(Long id, AnimeRequest request) {
        Anime findAnime = animeRepository.findById(id)
                .orElseThrow(() -> new AnimeNotFoundException(id));

        if (animeRepository.existsByTitleRomajiAndIdNot(request.titleRomaji(), id)) {
            throw new AnimeExistsException(request.titleRomaji());
        }

        findAnime.setTitleRomaji(request.titleRomaji());
        findAnime.setTitleEnglish(request.titleEnglish());
        findAnime.setType(request.type());
        findAnime.setEpisodeCount(request.episodeCount());
        findAnime.setStatus(request.status());
        findAnime.setSynopsis(request.synopsis());

        Anime updatedAnime = animeRepository.save(findAnime);
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
        return AnimeResponse.builder()
                .id(anime.getId())
                .titleRomaji(anime.getTitleRomaji())
                .titleEnglish(anime.getTitleEnglish())
                .type(anime.getType())
                .episodeCount(anime.getEpisodeCount())
                .status(anime.getStatus())
                .synopsis(anime.getSynopsis())
                .createdAt(anime.getCreatedAt())
                .updatedAt(anime.getUpdatedAt())
                .build();
    }
}