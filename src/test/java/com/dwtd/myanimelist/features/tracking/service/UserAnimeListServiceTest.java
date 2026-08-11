package com.dwtd.myanimelist.features.tracking.service;

import com.dwtd.myanimelist.exception.anime.AnimeNotFoundException;
import com.dwtd.myanimelist.exception.tracking.InvalidUserAnimeDataException;
import com.dwtd.myanimelist.exception.tracking.UserAnimeListAlreadyExistsException;
import com.dwtd.myanimelist.exception.tracking.UserAnimeListNotFoundException;
import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.anime.enums.AnimeStatus;
import com.dwtd.myanimelist.features.anime.enums.AnimeType;
import com.dwtd.myanimelist.features.anime.repository.AnimeRepository;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.auth.service.UserService;
import com.dwtd.myanimelist.features.tracking.dto.AddUserAnimeListRequest;
import com.dwtd.myanimelist.features.tracking.dto.UpdateUserAnimeListRequest;
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

import java.time.Instant;
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
    void addAnimeToList_shouldCreateEntryWithWatchingAndZeroEpisodes_whenValid() {
        AddUserAnimeListRequest request = new AddUserAnimeListRequest(1L);

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(animeRepository.findById(1L)).thenReturn(Optional.of(testAnime));
        when(userAnimeListRepository.existsByUserIdAndAnimeId(1L, 1L)).thenReturn(false);

        UserAnimeList saved = UserAnimeList.builder()
                .id(10L)
                .user(testUser)
                .anime(testAnime)
                .status(UserAnimeStatus.WATCHING)
                .score(null)
                .watchedEpisodes(0)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userAnimeListRepository.save(any(UserAnimeList.class))).thenReturn(saved);

        UserAnimeListResponse response = service.addAnimeToList(request);

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(UserAnimeStatus.WATCHING);
        assertThat(response.score()).isNull();
        assertThat(response.watchedEpisodes()).isZero();
        assertThat(response.totalEpisodes()).isEqualTo(220);
        verify(userAnimeListRepository).save(any(UserAnimeList.class));
    }

    @Test
    void addAnimeToList_shouldThrowUserAnimeListAlreadyExistsException_whenEntryExists() {
        AddUserAnimeListRequest request = new AddUserAnimeListRequest(1L);

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(animeRepository.findById(1L)).thenReturn(Optional.of(testAnime));
        when(userAnimeListRepository.existsByUserIdAndAnimeId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.addAnimeToList(request))
                .isInstanceOf(UserAnimeListAlreadyExistsException.class)
                .hasMessageContaining("Anime with id 1 already exists in user's list");
        verify(userAnimeListRepository, never()).save(any());
    }

    @Test
    void addAnimeToList_shouldThrowAnimeNotFoundException_whenAnimeNotExists() {
        AddUserAnimeListRequest request = new AddUserAnimeListRequest(99L);

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(animeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addAnimeToList(request))
                .isInstanceOf(AnimeNotFoundException.class)
                .hasMessageContaining("Anime with id 99 not found");
        verify(userAnimeListRepository, never()).save(any());
    }

    @Test
    void getUserList_shouldReturnFilteredList_whenStatusProvided() {
        String username = "TestUser";
        String status = "WATCHING";

        when(userService.getByUsername(username)).thenReturn(testUser);

        UserAnimeList entry1 = UserAnimeList.builder()
                .id(1L)
                .user(testUser)
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
                .user(testUser)
                .anime(anime2)
                .status(UserAnimeStatus.COMPLETED)
                .score(9)
                .watchedEpisodes(87)
                .build();

        when(userAnimeListRepository.findByUserId(testUser.getId())).thenReturn(List.of(entry1, entry2));

        List<UserAnimeListSummary> result = service.getUserList(username, status);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).animeId()).isEqualTo(1L);
        assertThat(result.get(0).status()).isEqualTo(UserAnimeStatus.WATCHING);
        verify(userService).getByUsername(username);
        verify(userAnimeListRepository).findByUserId(testUser.getId());
    }

    @Test
    void getUserList_shouldReturnAll_whenStatusIsNull() {
        String username = "TestUser";

        when(userService.getByUsername(username)).thenReturn(testUser);

        UserAnimeList entry1 = UserAnimeList.builder()
                .id(1L)
                .user(testUser)
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
                .user(testUser)
                .anime(anime2)
                .status(UserAnimeStatus.COMPLETED)
                .score(9)
                .watchedEpisodes(87)
                .build();

        when(userAnimeListRepository.findByUserId(testUser.getId())).thenReturn(List.of(entry1, entry2));

        List<UserAnimeListSummary> result = service.getUserList(username, null);

        assertThat(result).hasSize(2);
        verify(userAnimeListRepository).findByUserId(testUser.getId());
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
    void updateAnimeInList_shouldUpdateStatusScoreAndWatched_whenAllFieldsProvided() {
        Long animeId = 1L;
        UpdateUserAnimeListRequest request = new UpdateUserAnimeListRequest(
                UserAnimeStatus.COMPLETED, 10, 220
        );

        UserAnimeList existing = UserAnimeList.builder()
                .id(10L)
                .user(testUser)
                .anime(testAnime)
                .status(UserAnimeStatus.WATCHING)
                .score(8)
                .watchedEpisodes(5)
                .build();

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(animeRepository.findById(animeId)).thenReturn(Optional.of(testAnime));
        when(userAnimeListRepository.findByUserIdAndAnimeId(testUser.getId(), animeId))
                .thenReturn(Optional.of(existing));
        when(userAnimeListRepository.save(existing)).thenReturn(existing);

        UserAnimeListResponse response = service.updateAnimeInList(animeId, request);

        assertThat(response.status()).isEqualTo(UserAnimeStatus.COMPLETED);
        assertThat(response.score()).isEqualTo(10);
        assertThat(response.watchedEpisodes()).isEqualTo(220);
        verify(userAnimeListRepository).save(existing);
    }

    @Test
    void updateAnimeInList_shouldAutoSetWatchedToMax_whenStatusCompletedAndWatchedNotProvided() {
        Long animeId = 1L;
        UpdateUserAnimeListRequest request = new UpdateUserAnimeListRequest(
                UserAnimeStatus.COMPLETED, null, null
        );

        UserAnimeList existing = UserAnimeList.builder()
                .id(10L)
                .user(testUser)
                .anime(testAnime)
                .status(UserAnimeStatus.WATCHING)
                .score(8)
                .watchedEpisodes(5)
                .build();

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(animeRepository.findById(animeId)).thenReturn(Optional.of(testAnime));
        when(userAnimeListRepository.findByUserIdAndAnimeId(testUser.getId(), animeId))
                .thenReturn(Optional.of(existing));
        when(userAnimeListRepository.save(existing)).thenReturn(existing);

        UserAnimeListResponse response = service.updateAnimeInList(animeId, request);

        assertThat(response.status()).isEqualTo(UserAnimeStatus.COMPLETED);
        assertThat(response.watchedEpisodes()).isEqualTo(220); // max episodes
        verify(userAnimeListRepository).save(existing);
    }

    @Test
    void updateAnimeInList_shouldThrowInvalidUserAnimeDataException_whenWatchedExceedsTotal() {
        Long animeId = 1L;
        UpdateUserAnimeListRequest request = new UpdateUserAnimeListRequest(
                UserAnimeStatus.WATCHING, null, 300
        );

        UserAnimeList existing = UserAnimeList.builder()
                .id(10L)
                .user(testUser)
                .anime(testAnime)
                .status(UserAnimeStatus.WATCHING)
                .score(8)
                .watchedEpisodes(5)
                .build();

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(animeRepository.findById(animeId)).thenReturn(Optional.of(testAnime));
        when(userAnimeListRepository.findByUserIdAndAnimeId(testUser.getId(), animeId))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.updateAnimeInList(animeId, request))
                .isInstanceOf(InvalidUserAnimeDataException.class)
                .hasMessageContaining("Watched episodes cannot exceed total episodes (220)");
        verify(userAnimeListRepository, never()).save(any());
    }

    @Test
    void updateAnimeInList_shouldThrowAnimeNotFoundException_whenAnimeNotExists() {
        Long animeId = 99L;
        UpdateUserAnimeListRequest request = new UpdateUserAnimeListRequest(
                UserAnimeStatus.WATCHING, null, null
        );

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(animeRepository.findById(animeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAnimeInList(animeId, request))
                .isInstanceOf(AnimeNotFoundException.class)
                .hasMessageContaining("Anime with id 99 not found");
        verify(userAnimeListRepository, never()).save(any());
    }

    @Test
    void updateAnimeInList_shouldThrowUserAnimeListNotFoundException_whenEntryNotExists() {
        Long animeId = 1L;
        UpdateUserAnimeListRequest request = new UpdateUserAnimeListRequest(
                UserAnimeStatus.WATCHING, null, null
        );

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(animeRepository.findById(animeId)).thenReturn(Optional.of(testAnime));
        when(userAnimeListRepository.findByUserIdAndAnimeId(testUser.getId(), animeId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAnimeInList(animeId, request))
                .isInstanceOf(UserAnimeListNotFoundException.class)
                .hasMessageContaining("not found");
        verify(userAnimeListRepository, never()).save(any());
    }

    @Test
    void delete_shouldDelete_whenEntryExists() {
        Long animeId = 10L;
        Long currentUserId = testUser.getId();

        UserAnimeList entry = UserAnimeList.builder()
                .id(10L)
                .user(testUser)
                .anime(testAnime)
                .status(UserAnimeStatus.WATCHING)
                .score(8)
                .watchedEpisodes(5)
                .build();

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(userAnimeListRepository.findByUserIdAndAnimeId(currentUserId, animeId))
                .thenReturn(Optional.of(entry));

        service.delete(animeId);

        verify(userAnimeListRepository).delete(entry);
        verify(userAnimeListRepository, never()).deleteByUserIdAndAnimeId(any(), any());
    }

    @Test
    void delete_shouldThrowUserAnimeListNotFoundException_whenEntryNotExists() {
        Long animeId = 99L;
        Long currentUserId = testUser.getId();

        when(userService.getCurrentUser()).thenReturn(testUser);
        when(userAnimeListRepository.findByUserIdAndAnimeId(currentUserId, animeId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(animeId))
                .isInstanceOf(UserAnimeListNotFoundException.class)
                .hasMessageContaining("not found");
        verify(userAnimeListRepository, never()).deleteByUserIdAndAnimeId(any(), any());
    }
}