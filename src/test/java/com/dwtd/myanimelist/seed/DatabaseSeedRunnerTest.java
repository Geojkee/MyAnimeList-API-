package com.dwtd.myanimelist.seed;

import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.anime.enums.AnimeStatus;
import com.dwtd.myanimelist.features.anime.enums.AnimeType;
import com.dwtd.myanimelist.features.anime.repository.AnimeRepository;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.auth.repository.UserRepository;
import com.dwtd.myanimelist.features.genre.entity.Genre;
import com.dwtd.myanimelist.features.genre.repository.GenreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseSeedRunnerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private AnimeRepository animeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DatabaseSeedRunner runner;

    @BeforeEach
    void setUp() {
        runner = runnerWithPassword("local-admin");
    }

    @Test
    void isExcludedFromTheTestProfile() {
        Profile profile = DatabaseSeedRunner.class.getAnnotation(Profile.class);
        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("!test");
    }

    @Test
    void skipsWhenAdminAlreadyExists() {
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        runner.run(null);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(genreRepository, animeRepository, passwordEncoder);
    }

    @Test
    void skipsWhenAdminEmailAlreadyExists() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@myanimelist.local")).thenReturn(true);

        runner.run(null);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(genreRepository, animeRepository, passwordEncoder);
    }

    @Test
    void skipsWhenGenresAlreadyExist() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(genreRepository.count()).thenReturn(2L);

        runner.run(null);

        verify(userRepository, never()).save(any());
        verify(animeRepository, never()).count();
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void skipsWhenAnimeAlreadyExist() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(genreRepository.count()).thenReturn(0L);
        when(animeRepository.count()).thenReturn(3L);

        runner.run(null);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void rejectsPasswordThatCannotBeUsedAtSignIn() {
        DatabaseSeedRunner invalid = runnerWithPassword("short");
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(genreRepository.count()).thenReturn(0L);
        when(animeRepository.count()).thenReturn(0L);

        assertThatThrownBy(() -> invalid.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SEED_ADMIN_PASSWORD");

        verify(userRepository, never()).save(any());
    }

    @Test
    void seedsAdminGenresAndOneHundredAnimeWhenDatabaseIsEmpty() {
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(genreRepository.count()).thenReturn(0L);
        when(animeRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("local-admin")).thenReturn("encoded-hash");
        when(genreRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(animeRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        runner.run(null);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Genre>> genreCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(genreRepository).saveAll(genreCaptor.capture());
        List<String> genreNames = new ArrayList<>();
        genreCaptor.getValue().forEach(genre -> genreNames.add(genre.getName()));
        assertThat(genreNames).containsExactly(
                "Action",
                "Adventure",
                "Comedy",
                "Drama",
                "Fantasy",
                "Horror",
                "Mecha",
                "Music",
                "Mystery",
                "Romance",
                "Sci-Fi",
                "Slice of Life",
                "Sports",
                "Supernatural",
                "Thriller"
        );

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<Anime>> animeCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(animeRepository).saveAll(animeCaptor.capture());
        List<Anime> anime = new ArrayList<>();
        animeCaptor.getValue().forEach(anime::add);

        assertThat(anime).hasSize(DatabaseSeedRunner.ANIME_COUNT);
        assertThat(anime).extracting(Anime::getTitleRomaji).doesNotHaveDuplicates();
        assertThat(anime).extracting(Anime::getType)
                .contains(AnimeType.TV, AnimeType.MOVIE, AnimeType.OVA, AnimeType.SPECIAL);
        assertThat(anime).allSatisfy(row -> {
            assertThat(row.getStatus()).isEqualTo(AnimeStatus.FINISHED);
            assertThat(row.getEpisodeCount()).isPositive();
            assertThat(row.getGenres()).isNotEmpty();
            assertThat(row.getReleaseDate()).isNotNull();
            assertThat(row.getReleaseDate().getMonthValue()).isEqualTo(1);
            assertThat(row.getReleaseDate().getDayOfMonth()).isEqualTo(1);
            assertThat(row.getSynopsis()).contains(" ");
            assertThat(row.getSynopsis()).doesNotContain("premiered in");
        });
        assertThat(anime).filteredOn(row -> row.getType() == AnimeType.MOVIE)
                .allSatisfy(row -> assertThat(row.getEpisodeCount()).isEqualTo(1));

        Set<String> usedGenres = new HashSet<>();
        anime.forEach(row -> row.getGenres().forEach(genre -> usedGenres.add(genre.getName())));
        assertThat(usedGenres).containsExactlyInAnyOrderElementsOf(genreNames);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User admin = userCaptor.getValue();
        assertThat(admin.getUsername()).isEqualTo("admin");
        assertThat(admin.getEmail()).isEqualTo("admin@myanimelist.local");
        assertThat(admin.getPassword()).isEqualTo("encoded-hash");
        assertThat(admin.getRole()).isEqualTo(Role.ROLE_ADMIN);
    }

    private DatabaseSeedRunner runnerWithPassword(String password) {
        return new DatabaseSeedRunner(
                userRepository,
                genreRepository,
                animeRepository,
                passwordEncoder,
                "admin",
                "admin@myanimelist.local",
                password
        );
    }
}
