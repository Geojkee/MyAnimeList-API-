package com.dwtd.myanimelist.features.tracking.service;

import com.dwtd.myanimelist.exception.anime.AnimeNotFoundException;
import com.dwtd.myanimelist.exception.tracking.InvalidUserAnimeDataException;
import com.dwtd.myanimelist.exception.tracking.UserAnimeListNotFoundException;
import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.anime.enums.AnimeStatus;
import com.dwtd.myanimelist.features.anime.enums.AnimeType;
import com.dwtd.myanimelist.features.anime.repository.AnimeRepository;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.auth.service.UserService;
import com.dwtd.myanimelist.features.tracking.dto.UserAnimeListRequest;
import com.dwtd.myanimelist.features.tracking.dto.UserAnimeListResponse;
import com.dwtd.myanimelist.features.tracking.dto.UserAnimeListSummary;
import com.dwtd.myanimelist.features.tracking.entity.UserAnimeList;
import com.dwtd.myanimelist.features.tracking.enums.UserAnimeStatus;
import com.dwtd.myanimelist.features.tracking.repository.UserAnimeListRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAnimeListServiceTest {

    @Mock
    private UserAnimeListRepository userAnimeListRepository;

    @Mock
    private AnimeRepository animeRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserAnimeListService service;

    private final User testUser = User.builder()
            .id(1L)
            .username("TestUser")
            .email("test@example.com")
            .password("encoded")
            .role(Role.ROLE_USER)
            .build();

    private final Anime testAnime = Anime.builder()
            .id(1L)
            .titleRomaji("Naruto")
            .type(AnimeType.TV)
            .episodeCount(220)
            .status(AnimeStatus.FINISHED)
            .build();


    @Test
    void addOrUpdate_shouldCreateNewEntry_whenNotExists() {
        UserAnimeListRequest request = new UserAnimeListRequest(1L, UserAnimeStatus.WATCHING, 8, 5);

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(animeRepository.findById(1L)).thenReturn(Optional.of(testAnime));
        when(userAnimeListRepository.findByUserIdAndAnimeId(1L, 1L)).thenReturn(Optional.empty());

        UserAnimeList saved = UserAnimeList.builder()
                .id(10L)
                .user(testUser)
                .anime(testAnime)
                .status(UserAnimeStatus.WATCHING)
                .score(8)
                .watchedEpisodes(5)
                .build();

        when(userAnimeListRepository.save(any(UserAnimeList.class))).thenReturn(saved);

        UserAnimeListResponse response = service.addOrUpdate(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.animeId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(UserAnimeStatus.WATCHING);
        assertThat(response.score()).isEqualTo(8);
        assertThat(response.watchedEpisodes()).isEqualTo(5);
        assertThat(response.totalEpisodes()).isEqualTo(220);

        verify(userAnimeListRepository).save(any(UserAnimeList.class));
    }

    @Test
    void addOrUpdate_shouldUpdateExistingEntry_whenExists() {
        UserAnimeListRequest request = new UserAnimeListRequest(1L, UserAnimeStatus.COMPLETED, 10, 220);

        UserAnimeList existing = UserAnimeList.builder()
                .id(10L)
                .user(testUser)
                .anime(testAnime)
                .status(UserAnimeStatus.WATCHING)
                .score(8)
                .watchedEpisodes(5)
                .build();

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(animeRepository.findById(1L)).thenReturn(Optional.of(testAnime));
        when(userAnimeListRepository.findByUserIdAndAnimeId(1L, 1L)).thenReturn(Optional.of(existing));
        when(userAnimeListRepository.save(existing)).thenReturn(existing);

        UserAnimeListResponse response = service.addOrUpdate(request);

        assertThat(response.status()).isEqualTo(UserAnimeStatus.COMPLETED);
        assertThat(response.score()).isEqualTo(10);
        assertThat(response.watchedEpisodes()).isEqualTo(220);
    }

    @Test
    void addOrUpdate_shouldThrowAnimeNotFoundException_whenAnimeNotExists() {
        UserAnimeListRequest request = new UserAnimeListRequest(99L, UserAnimeStatus.WATCHING, null, null);

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(animeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addOrUpdate(request))
                .isInstanceOf(AnimeNotFoundException.class)
                .hasMessageContaining("Anime with id 99 not found");
    }

    @Test
    void addOrUpdate_shouldThrowInvalidUserAnimeDataException_whenWatchedEpisodesExceedsTotal() {
        UserAnimeListRequest request = new UserAnimeListRequest(1L, UserAnimeStatus.WATCHING, 8, 300);

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(animeRepository.findById(1L)).thenReturn(Optional.of(testAnime));

        assertThatThrownBy(() -> service.addOrUpdate(request))
                .isInstanceOf(InvalidUserAnimeDataException.class)
                .hasMessageContaining("Watched episodes cannot exceed total episodes (220)");
    }

    @Test
    void getUserList_shouldReturnFilteredList_whenStatusProvided() {
        String username = "TestUser";
        String status = "WATCHING";

        User user = testUser;
        when(userService.getByUsername(username)).thenReturn(user);

        UserAnimeList entry1 = UserAnimeList.builder()
                .id(1L)
                .user(user)
                .anime(testAnime)
                .status(UserAnimeStatus.WATCHING)
                .score(8)
                .watchedEpisodes(5)
                .build();

        Anime anime2 = Anime.builder()
                .id(2L)
                .titleRomaji("Attack on Titan")
                .episodeCount(87)
                .build();

        UserAnimeList entry2 = UserAnimeList.builder()
                .id(2L)
                .user(user)
                .anime(anime2)
                .status(UserAnimeStatus.COMPLETED)
                .score(9)
                .watchedEpisodes(87)
                .build();

        when(userAnimeListRepository.findByUserId(user.getId())).thenReturn(List.of(entry1, entry2));

        List<UserAnimeListSummary> result = service.getUserList(username, status);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).animeId()).isEqualTo(1L);
        assertThat(result.get(0).status()).isEqualTo(UserAnimeStatus.WATCHING);

        verify(userService).getByUsername(username);
        verify(userAnimeListRepository).findByUserId(user.getId());
    }

    @Test
    void getUserList_shouldReturnAll_whenStatusIsNull() {
        String username = "TestUser";

        User user = testUser;
        when(userService.getByUsername(username)).thenReturn(user);

        UserAnimeList entry1 = UserAnimeList.builder()
                .id(1L)
                .user(user)
                .anime(testAnime)
                .status(UserAnimeStatus.WATCHING)
                .score(8)
                .watchedEpisodes(5)
                .build();

        Anime anime2 = Anime.builder()
                .id(2L)
                .titleRomaji("Attack on Titan")
                .episodeCount(87)
                .build();

        UserAnimeList entry2 = UserAnimeList.builder()
                .id(2L)
                .user(user)
                .anime(anime2)
                .status(UserAnimeStatus.COMPLETED)
                .score(9)
                .watchedEpisodes(87)
                .build();

        when(userAnimeListRepository.findByUserId(user.getId())).thenReturn(List.of(entry1, entry2));

        List<UserAnimeListSummary> result = service.getUserList(username, null);

        assertThat(result).hasSize(2);
        verify(userAnimeListRepository).findByUserId(user.getId());
    }

    @Test
    void getUserList_shouldThrowUserNotFoundException_whenUserNotExists() {
        String username = "NonExistent";
        when(userService.getByUsername(username)).thenThrow(new RuntimeException("User not found"));

        assertThatThrownBy(() -> service.getUserList(username, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void remove_shouldDelete_whenExists() {
        Long userId = 1L;
        Long animeId = 1L;
        when(userAnimeListRepository.existsByUserIdAndAnimeId(userId, animeId)).thenReturn(true);

        service.remove(userId, animeId);

        verify(userAnimeListRepository).deleteByUserIdAndAnimeId(userId, animeId);
    }

    @Test
    void remove_shouldThrowUserAnimeListNotFoundException_whenNotExists() {
        Long userId = 1L;
        Long animeId = 99L;
        when(userAnimeListRepository.existsByUserIdAndAnimeId(userId, animeId)).thenReturn(false);

        assertThatThrownBy(() -> service.remove(userId, animeId))
                .isInstanceOf(UserAnimeListNotFoundException.class)
                .hasMessageContaining("Anime with id 99 not found in user's list");

        verify(userAnimeListRepository, never()).deleteByUserIdAndAnimeId(any(), any());
    }
}