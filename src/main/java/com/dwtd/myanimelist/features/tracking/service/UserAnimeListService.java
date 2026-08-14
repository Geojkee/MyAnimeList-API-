package com.dwtd.myanimelist.features.tracking.service;

import com.dwtd.myanimelist.exception.anime.AnimeNotFoundException;
import com.dwtd.myanimelist.exception.tracking.InvalidUserAnimeDataException;
import com.dwtd.myanimelist.exception.tracking.UserAnimeListAlreadyExistsException;
import com.dwtd.myanimelist.exception.tracking.UserAnimeListNotFoundException;
import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.anime.repository.AnimeRepository;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.service.UserService;
import com.dwtd.myanimelist.features.tracking.dto.AddUserAnimeListRequest;
import com.dwtd.myanimelist.features.tracking.dto.UpdateUserAnimeListRequest;
import com.dwtd.myanimelist.features.tracking.dto.UserAnimeListResponse;
import com.dwtd.myanimelist.features.tracking.dto.UserAnimeListSummary;
import com.dwtd.myanimelist.features.tracking.entity.UserAnimeList;
import com.dwtd.myanimelist.features.tracking.enums.UserAnimeStatus;
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
    public UserAnimeListResponse addAnimeToList(AddUserAnimeListRequest request) {
        User user = userService.getCurrentUser();

        Anime anime = animeRepository.findById(request.animeId())
                .orElseThrow(() -> new AnimeNotFoundException(request.animeId()));

        if (userAnimeListRepository.existsByUserIdAndAnimeId(user.getId(), anime.getId())) {
            throw new UserAnimeListAlreadyExistsException(anime.getId());
        }

        UserAnimeList newEntry = UserAnimeList.builder()
                .user(user)
                .anime(anime)
                .status(UserAnimeStatus.WATCHING)
                .score(null)
                .watchedEpisodes(0)
                .build();

        UserAnimeList saved = userAnimeListRepository.save(newEntry);
        log.info("Added anime {} to user {} list", anime.getTitleRomaji(), user.getUsername());
        return mapToResponse(saved);
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
    public UserAnimeListResponse updateAnimeInList(Long animeId, UpdateUserAnimeListRequest request){
        User user = userService.getCurrentUser();

        Anime anime = animeRepository.findById(animeId)
                .orElseThrow(() -> new AnimeNotFoundException(animeId));

        UserAnimeList entry = userAnimeListRepository.findByUserIdAndAnimeId(user.getId(), animeId)
                .orElseThrow(() -> new UserAnimeListNotFoundException(animeId));

        if (request.status() != null) {
            entry.setStatus(request.status());
        }

        if (request.score() != null) {
            entry.setScore(request.score());
        }   

        Integer newWatched = request.watchedEpisodes();

        if (request.status() == UserAnimeStatus.COMPLETED && newWatched == null) {
            newWatched = anime.getEpisodeCount();
        }

        if (newWatched != null) {
            if (newWatched > anime.getEpisodeCount()) {
                throw new InvalidUserAnimeDataException(
                        "Watched episodes cannot exceed total episodes (" + anime.getEpisodeCount() + ")"
                );
            }
            entry.setWatchedEpisodes(newWatched);
        }

        UserAnimeList updated = userAnimeListRepository.save(entry);
        log.info("Updated anime {} in user {} list", anime.getTitleRomaji(), user.getUsername());
        return mapToResponse(updated);
    }

    @Transactional
    public void delete(Long animeId) {
        User currentUser = userService.getCurrentUser();

        UserAnimeList entry = userAnimeListRepository
                .findByUserIdAndAnimeId(currentUser.getId(), animeId)
                .orElseThrow(() -> new UserAnimeListNotFoundException(animeId));

        userAnimeListRepository.delete(entry);
        log.info("Removed anime {} from user {} list", animeId, currentUser.getUsername());
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