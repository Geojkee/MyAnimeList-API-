package com.dwtd.myanimelist.features.tracking.service;

import com.dwtd.myanimelist.exception.anime.AnimeNotFoundException;
import com.dwtd.myanimelist.exception.tracking.InvalidUserAnimeDataException;
import com.dwtd.myanimelist.exception.tracking.UserAnimeListNotFoundException;
import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.anime.repository.AnimeRepository;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.service.UserService;
import com.dwtd.myanimelist.features.tracking.dto.UserAnimeListRequest;
import com.dwtd.myanimelist.features.tracking.dto.UserAnimeListResponse;
import com.dwtd.myanimelist.features.tracking.dto.UserAnimeListSummary;
import com.dwtd.myanimelist.features.tracking.entity.UserAnimeList;
import com.dwtd.myanimelist.features.tracking.repository.UserAnimeListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAnimeListService {

    private final UserAnimeListRepository userAnimeListRepository;
    private final AnimeRepository animeRepository;
    private final UserService userService;

    @Transactional
    public UserAnimeListResponse addOrUpdate(UserAnimeListRequest request) {
        User user = userService.getCurrentUser();

        Anime anime = animeRepository.findById(request.animeId())
                .orElseThrow(() -> new AnimeNotFoundException(request.animeId()));

        if (request.watchedEpisodes() != null && request.watchedEpisodes() > anime.getEpisodeCount()) {
            throw new InvalidUserAnimeDataException(
                    "Watched episodes cannot exceed total episodes (" + anime.getEpisodeCount() + ")"
            );
        }

        UserAnimeList existing = userAnimeListRepository
                .findByUserIdAndAnimeId(user.getId(), anime.getId())
                .orElse(null);

        int watchedEpisodes = request.watchedEpisodes() != null ? request.watchedEpisodes() : 0;

        if (existing != null) {
            existing.setStatus(request.status());
            existing.setScore(request.score());
            existing.setWatchedEpisodes(watchedEpisodes);
            userAnimeListRepository.save(existing);
            log.info("Updated anime list for user {}: anime={}, status={}",
                    user.getUsername(), anime.getTitleRomaji(), request.status());
            return mapToResponse(existing);
        } else {
            UserAnimeList newEntry = UserAnimeList.builder()
                    .user(user)
                    .anime(anime)
                    .status(request.status())
                    .score(request.score())
                    .watchedEpisodes(watchedEpisodes)
                    .build();
            UserAnimeList saved = userAnimeListRepository.save(newEntry);
            log.info("Added anime to list for user {}: anime={}, status={}",
                    user.getUsername(), anime.getTitleRomaji(), request.status());
            return mapToResponse(saved);
        }
    }

    @Transactional(readOnly = true)
    public List<UserAnimeListSummary> getUserList(String username, String status) {
        User user = userService.getByUsername(username);
        List<UserAnimeList> entries = userAnimeListRepository.findByUserId(user.getId());

        return entries.stream()
                .filter(entry -> status == null || entry.getStatus().name().equalsIgnoreCase(status))
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    @Transactional
    public void remove(Long userId, Long animeId) {
        if (!userAnimeListRepository.existsByUserIdAndAnimeId(userId, animeId)) {
            throw new UserAnimeListNotFoundException(userId, animeId);
        }
        userAnimeListRepository.deleteByUserIdAndAnimeId(userId, animeId);
        log.info("Removed anime {} from user {} list", animeId, userId);
    }

    private UserAnimeListResponse mapToResponse(UserAnimeList entry) {
        return UserAnimeListResponse.builder()
                .id(entry.getId())
                .userId(entry.getUser().getId())
                .animeId(entry.getAnime().getId())
                .titleRomaji(entry.getAnime().getTitleRomaji())
                .titleEnglish(entry.getAnime().getTitleEnglish())
                .status(entry.getStatus())
                .score(entry.getScore())
                .watchedEpisodes(entry.getWatchedEpisodes())
                .totalEpisodes(entry.getAnime().getEpisodeCount())
                .synopsis(entry.getAnime().getSynopsis())
                .build();
    }

    private UserAnimeListSummary mapToSummary(UserAnimeList entry) {
        return UserAnimeListSummary.builder()
                .animeId(entry.getAnime().getId())
                .titleRomaji(entry.getAnime().getTitleRomaji())
                .titleEnglish(entry.getAnime().getTitleEnglish())
                .status(entry.getStatus())
                .score(entry.getScore())
                .watchedEpisodes(entry.getWatchedEpisodes())
                .totalEpisodes(entry.getAnime().getEpisodeCount())
                .build();
    }
}