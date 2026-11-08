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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Fills an empty database once, on application startup.
 * Skips when the configured admin already exists, or when genres or anime are already stored.
 * Not loaded under the {@code test} profile.
 */
@Slf4j
@Component
@Profile("!test")
public class DatabaseSeedRunner implements ApplicationRunner {

    static final int ANIME_COUNT = 100;

    private static final List<String> GENRES = List.of(
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

    private final UserRepository userRepository;
    private final GenreRepository genreRepository;
    private final AnimeRepository animeRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;

    public DatabaseSeedRunner(
            UserRepository userRepository,
            GenreRepository genreRepository,
            AnimeRepository animeRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.admin-username}") String adminUsername,
            @Value("${app.seed.admin-email}") String adminEmail,
            @Value("${app.seed.admin-password}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.genreRepository = genreRepository;
        this.animeRepository = animeRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (alreadySeeded()) {
            log.info("Database seed skipped: existing data detected");
            return;
        }

        validateAdminCredentials();
        List<SeedAnime> rows = catalog();
        validateCatalog(rows);

        Map<String, Genre> genresByName = seedGenres();
        seedAnime(rows, genresByName);
        seedAdmin();

        log.info(
                "Seeded admin '{}', {} genres, and {} anime",
                adminUsername,
                genresByName.size(),
                rows.size()
        );
    }

    private boolean alreadySeeded() {
        if (userRepository.existsByUsername(adminUsername) || userRepository.existsByEmail(adminEmail)) {
            return true;
        }
        if (genreRepository.count() > 0) {
            return true;
        }
        return animeRepository.count() > 0;
    }

    private void validateAdminCredentials() {
        if (adminUsername == null || adminUsername.length() < 4 || adminUsername.length() > 24) {
            throw new IllegalStateException("SEED_ADMIN_USERNAME must be 4 to 24 characters");
        }
        if (adminEmail == null || adminEmail.isBlank() || !adminEmail.contains("@")) {
            throw new IllegalStateException("SEED_ADMIN_EMAIL must be an email address");
        }
        if (adminPassword == null
                || adminPassword.length() < 8
                || adminPassword.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalStateException(
                    "SEED_ADMIN_PASSWORD must be at least 8 characters and contain no spaces"
            );
        }
    }

    private Map<String, Genre> seedGenres() {
        List<Genre> genres = GENRES.stream()
                .map(name -> Genre.builder().name(name).build())
                .toList();
        return genreRepository.saveAll(genres).stream()
                .collect(Collectors.toMap(Genre::getName, Function.identity()));
    }

    private void seedAnime(List<SeedAnime> rows, Map<String, Genre> genresByName) {
        List<Anime> anime = rows.stream()
                .map(row -> toAnime(row, genresByName))
                .toList();
        animeRepository.saveAll(anime);
    }

    private Anime toAnime(SeedAnime row, Map<String, Genre> genresByName) {
        Set<Genre> genres = Arrays.stream(row.genres())
                .map(genresByName::get)
                .collect(Collectors.toCollection(HashSet::new));

        Anime anime = Anime.builder()
                .titleRomaji(row.titleRomaji())
                .titleEnglish(row.titleEnglish())
                .type(row.type())
                .episodeCount(row.episodeCount())
                .status(row.status())
                .synopsis(synopsis(row))
                // Catalog stores the premiere year; the day is unknown, so the date is 1 January.
                .releaseDate(LocalDate.of(row.year(), 1, 1))
                .build();
        anime.setGenres(genres);
        return anime;
    }

    private void seedAdmin() {
        userRepository.save(User.builder()
                .username(adminUsername)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ROLE_ADMIN)
                .build());
    }

    private static String synopsis(SeedAnime row) {
        String kind = switch (row.type()) {
            case TV -> "TV series";
            case MOVIE -> "feature film";
            case OVA -> "original video animation";
            case SPECIAL -> "television special";
        };
        String state = switch (row.status()) {
            case FINISHED -> "Finished";
            case ONGOING -> "Ongoing";
            case ANNOUNCED -> "Announced";
        };
        return state + " " + kind + " " + row.titleEnglish() + ", premiered in " + row.year() + ".";
    }

    private static void validateCatalog(List<SeedAnime> rows) {
        if (rows.size() != ANIME_COUNT) {
            throw new IllegalStateException(
                    "Seed catalog must contain exactly " + ANIME_COUNT + " anime, found " + rows.size()
            );
        }

        Set<String> titles = new HashSet<>();
        Set<String> usedGenres = new HashSet<>();
        Set<String> knownGenres = new HashSet<>(GENRES);

        for (SeedAnime row : rows) {
            if (row.titleRomaji() == null || row.titleRomaji().isBlank()
                    || row.titleEnglish() == null || row.titleEnglish().isBlank()) {
                throw new IllegalStateException("Seed anime is missing a title");
            }
            if (!titles.add(row.titleRomaji())) {
                throw new IllegalStateException("Duplicate seed title: " + row.titleRomaji());
            }
            if (row.type() == null || row.status() == null) {
                throw new IllegalStateException("Seed anime is missing type or status: " + row.titleRomaji());
            }
            if (row.episodeCount() < 1 || (row.type() == AnimeType.MOVIE && row.episodeCount() != 1)) {
                throw new IllegalStateException("Unexpected episode count for " + row.titleRomaji());
            }
            if (row.year() < 1970 || row.year() > 2026) {
                throw new IllegalStateException("Unexpected premiere year for " + row.titleRomaji());
            }
            if (row.genres().length == 0) {
                throw new IllegalStateException("Seed anime has no genres: " + row.titleRomaji());
            }
            for (String genre : row.genres()) {
                if (!knownGenres.contains(genre)) {
                    throw new IllegalStateException(
                            "Unknown seed genre '" + genre + "' on " + row.titleRomaji()
                    );
                }
                usedGenres.add(genre);
            }
        }

        if (!usedGenres.equals(knownGenres)) {
            throw new IllegalStateException("Every seed genre must be used by at least one anime");
        }
    }

    private static List<SeedAnime> catalog() {
        return List.of(
                tv("Cowboy Bebop", "Cowboy Bebop", 26, 1998, "Action", "Sci-Fi"),
                tv("Shinseiki Evangelion", "Neon Genesis Evangelion", 26, 1995, "Mecha", "Drama", "Sci-Fi"),
                tv("Hagane no Renkinjutsushi", "Fullmetal Alchemist", 51, 2003, "Action", "Adventure", "Fantasy"),
                tv("Hagane no Renkinjutsushi: Fullmetal Alchemist", "Fullmetal Alchemist: Brotherhood", 64, 2009, "Action", "Adventure", "Fantasy", "Drama"),
                tv("Death Note", "Death Note", 37, 2006, "Mystery", "Supernatural", "Thriller"),
                tv("Steins;Gate", "Steins;Gate", 24, 2011, "Sci-Fi", "Thriller"),
                tv("Steins;Gate 0", "Steins;Gate 0", 23, 2018, "Sci-Fi", "Thriller", "Drama"),
                tv("Hunter x Hunter (2011)", "Hunter x Hunter", 148, 2011, "Action", "Adventure"),
                tv("Code Geass: Hangyaku no Lelouch", "Code Geass: Lelouch of the Rebellion", 25, 2006, "Action", "Mecha"),
                tv("Code Geass: Hangyaku no Lelouch R2", "Code Geass: Lelouch of the Rebellion R2", 25, 2008, "Action", "Mecha"),
                tv("Tengen Toppa Gurren Lagann", "Gurren Lagann", 27, 2007, "Action", "Mecha", "Adventure"),
                tv("Samurai Champloo", "Samurai Champloo", 26, 2004, "Action", "Adventure", "Comedy"),
                tv("Trigun", "Trigun", 26, 1998, "Action", "Sci-Fi", "Comedy"),
                tv("Monster", "Monster", 74, 2004, "Mystery", "Thriller", "Drama"),
                tv("Kenpuu Denki Berserk", "Berserk", 25, 1997, "Action", "Fantasy", "Horror"),
                tv("Koukaku Kidoutai: Stand Alone Complex", "Ghost in the Shell: Stand Alone Complex", 26, 2002, "Sci-Fi", "Action", "Mystery"),
                tv("Psycho-Pass", "Psycho-Pass", 22, 2012, "Sci-Fi", "Thriller", "Action"),
                tv("Ergo Proxy", "Ergo Proxy", 23, 2006, "Sci-Fi", "Mystery"),
                tv("Serial Experiments Lain", "Serial Experiments Lain", 13, 1998, "Sci-Fi", "Mystery", "Drama"),
                tv("Mou Sou Dairinin", "Paranoia Agent", 13, 2004, "Mystery", "Horror", "Thriller"),
                tv("Haibane Renmei", "Haibane Renmei", 13, 2002, "Fantasy", "Drama", "Mystery"),
                tv("Mushishi", "Mushishi", 26, 2005, "Fantasy", "Slice of Life", "Supernatural"),
                tv("Natsume Yuujinchou", "Natsume's Book of Friends", 13, 2008, "Slice of Life", "Supernatural"),
                tv("Barakamon", "Barakamon", 12, 2014, "Slice of Life", "Comedy"),
                tv("3-gatsu no Lion", "March Comes in Like a Lion", 22, 2016, "Drama", "Slice of Life"),
                tv("Clannad", "Clannad", 23, 2007, "Drama", "Romance", "Supernatural"),
                tv("Clannad: After Story", "Clannad: After Story", 24, 2008, "Drama", "Romance", "Supernatural"),
                tv("Toradora!", "Toradora!", 25, 2008, "Romance", "Comedy", "Slice of Life"),
                tv("Ano Hi Mita Hana no Namae wo Bokutachi wa Mada Shiranai.", "Anohana: The Flower We Saw That Day", 11, 2011, "Drama", "Supernatural"),
                tv("Shigatsu wa Kimi no Uso", "Your Lie in April", 22, 2014, "Drama", "Romance", "Music"),
                tv("Violet Evergarden", "Violet Evergarden", 13, 2018, "Drama", "Fantasy"),
                tv("Sora yori mo Tooi Basho", "A Place Further Than the Universe", 13, 2018, "Adventure", "Drama"),
                tv("Made in Abyss", "Made in Abyss", 13, 2017, "Adventure", "Fantasy", "Mystery"),
                tv("Odd Taxi", "Odd Taxi", 13, 2021, "Mystery", "Comedy"),
                tv("Sousou no Frieren", "Frieren: Beyond Journey's End", 28, 2023, "Adventure", "Fantasy", "Drama"),
                tv("Bocchi the Rock!", "Bocchi the Rock!", 12, 2022, "Comedy", "Music", "Slice of Life"),
                tv("K-On!", "K-On!", 13, 2009, "Comedy", "Music", "Slice of Life"),
                tv("Hibike! Euphonium", "Sound! Euphonium", 13, 2015, "Drama", "Music"),
                tv("Mob Psycho 100", "Mob Psycho 100", 12, 2016, "Action", "Comedy", "Supernatural"),
                tv("One Punch Man", "One Punch Man", 12, 2015, "Action", "Comedy"),
                tv("Vinland Saga", "Vinland Saga", 24, 2019, "Action", "Adventure", "Drama"),
                tv("Shingeki no Kyojin", "Attack on Titan", 25, 2013, "Action", "Drama", "Fantasy"),
                tv("Kimetsu no Yaiba", "Demon Slayer: Kimetsu no Yaiba", 26, 2019, "Action", "Fantasy", "Supernatural"),
                tv("Jujutsu Kaisen", "Jujutsu Kaisen", 24, 2020, "Action", "Supernatural"),
                tv("Chainsaw Man", "Chainsaw Man", 12, 2022, "Action", "Horror", "Supernatural"),
                tv("Boku no Hero Academia", "My Hero Academia", 13, 2016, "Action"),
                tv("Kaguya-sama wa Kokurasetai: Tensai-tachi no Renai Zunousen", "Kaguya-sama: Love Is War", 12, 2019, "Comedy", "Romance"),
                tv("Re:Zero kara Hajimeru Isekai Seikatsu", "Re:Zero - Starting Life in Another World", 25, 2016, "Fantasy", "Thriller", "Drama"),
                tv("Kono Subarashii Sekai ni Shukufuku wo!", "KonoSuba: God's Blessing on This Wonderful World!", 10, 2016, "Comedy", "Fantasy", "Adventure"),
                tv("Dungeon Meshi", "Delicious in Dungeon", 24, 2024, "Adventure", "Fantasy", "Comedy"),
                tv("Cyberpunk: Edgerunners", "Cyberpunk: Edgerunners", 10, 2022, "Action", "Sci-Fi"),
                tv("Baccano!", "Baccano!", 13, 2007, "Action", "Mystery"),
                tv("Durarara!!", "Durarara!!", 24, 2010, "Action", "Mystery", "Supernatural"),
                tv("Bungou Stray Dogs", "Bungo Stray Dogs", 12, 2016, "Action", "Mystery", "Supernatural"),
                tv("Noragami", "Noragami", 12, 2014, "Action", "Supernatural"),
                tv("Ping Pong the Animation", "Ping Pong the Animation", 11, 2014, "Sports", "Drama"),
                tv("Haikyuu!!", "Haikyu!!", 25, 2014, "Sports", "Comedy"),
                tv("Kaze ga Tsuyoku Fuiteiru", "Run with the Wind", 23, 2018, "Sports", "Drama"),
                tv("Hajime no Ippo", "Fighting Spirit", 75, 2000, "Sports", "Drama"),
                tv("Slam Dunk", "Slam Dunk", 101, 1993, "Sports", "Comedy"),
                tv("Initial D First Stage", "Initial D First Stage", 26, 1998, "Action", "Sports"),
                tv("Planetes", "Planetes", 26, 2003, "Sci-Fi", "Drama"),
                tv("Nichijou", "Nichijou - My Ordinary Life", 26, 2011, "Comedy", "Slice of Life"),
                tv("Hyouka", "Hyouka", 22, 2012, "Mystery", "Slice of Life"),
                tv("Nodame Cantabile", "Nodame Cantabile", 23, 2007, "Comedy", "Romance", "Music"),
                tv("Sakamichi no Apollon", "Kids on the Slope", 12, 2012, "Drama", "Music", "Romance"),
                tv("Boku dake ga Inai Machi", "Erased", 12, 2016, "Mystery", "Thriller"),
                tv("Zankyou no Terror", "Terror in Resonance", 11, 2014, "Mystery", "Thriller"),
                tv("Shinsekai yori", "From the New World", 25, 2012, "Drama", "Mystery", "Sci-Fi", "Horror"),
                tv("Kiseijuu: Sei no Kakuritsu", "Parasyte: The Maxim", 24, 2014, "Action", "Horror", "Sci-Fi"),
                tv("Kidou Senshi Gundam", "Mobile Suit Gundam", 43, 1979, "Mecha", "Action", "Drama"),
                tv("Tenkuu no Escaflowne", "The Vision of Escaflowne", 26, 1996, "Adventure", "Fantasy", "Mecha", "Romance"),
                tv("Koukyoushihen Eureka Seven", "Eureka Seven", 50, 2005, "Mecha", "Adventure", "Romance"),
                tv("Darker than Black: Kuro no Keiyakusha", "Darker than Black", 25, 2007, "Action", "Mystery", "Sci-Fi"),
                tv("Soul Eater", "Soul Eater", 51, 2008, "Action", "Fantasy", "Comedy"),
                tv("Yu Yu Hakusho", "Yu Yu Hakusho", 112, 1992, "Action", "Supernatural"),
                tv("Rurouni Kenshin: Meiji Kenkaku Romantan", "Rurouni Kenshin", 94, 1996, "Action", "Adventure", "Romance"),
                tv("Great Pretender", "Great Pretender", 23, 2020, "Action", "Comedy", "Thriller"),
                tv("Death Parade", "Death Parade", 12, 2015, "Drama", "Mystery"),
                tv("Chihayafuru", "Chihayafuru", 25, 2011, "Sports", "Drama"),
                movie("Sen to Chihiro no Kamikakushi", "Spirited Away", 2001, "Fantasy", "Adventure"),
                movie("Mononoke-hime", "Princess Mononoke", 1997, "Fantasy", "Adventure", "Action"),
                movie("Tonari no Totoro", "My Neighbor Totoro", 1988, "Fantasy", "Slice of Life"),
                movie("Kimi no Na wa.", "Your Name.", 2016, "Romance", "Drama", "Supernatural"),
                movie("Koe no Katachi", "A Silent Voice", 2016, "Drama"),
                movie("Howl no Ugoku Shiro", "Howl's Moving Castle", 2004, "Fantasy", "Adventure", "Romance"),
                movie("Hotaru no Haka", "Grave of the Fireflies", 1988, "Drama"),
                movie("Akira", "Akira", 1988, "Action", "Sci-Fi"),
                movie("Koukaku Kidoutai", "Ghost in the Shell", 1995, "Sci-Fi", "Action"),
                movie("Perfect Blue", "Perfect Blue", 1997, "Thriller", "Drama", "Mystery"),
                movie("Toki wo Kakeru Shoujo", "The Girl Who Leapt Through Time", 2006, "Sci-Fi", "Romance"),
                movie("Ookami Kodomo no Ame to Yuki", "Wolf Children", 2012, "Fantasy", "Slice of Life", "Drama"),
                movie("Tenki no Ko", "Weathering with You", 2019, "Romance", "Fantasy"),
                movie("Suzume no Tojimari", "Suzume", 2022, "Adventure", "Fantasy"),
                movie("Kimetsu no Yaiba: Mugen Ressha-hen", "Demon Slayer: Mugen Train", 2020, "Action", "Fantasy", "Supernatural"),
                ova("FLCL", "FLCL", 6, 2000, "Action", "Comedy", "Sci-Fi"),
                ova("Hellsing Ultimate", "Hellsing Ultimate", 10, 2006, "Action", "Horror", "Supernatural"),
                ova("Top wo Nerae! Gunbuster", "Gunbuster", 6, 1988, "Mecha", "Sci-Fi"),
                ova("Kidou Senshi Gundam 0080: Pocket no Naka no Sensou", "Mobile Suit Gundam 0080: War in the Pocket", 6, 1989, "Mecha", "Drama"),
                special("Shingeki no Kyojin: The Final Chapters", "Attack on Titan: The Final Chapters", 2, 2023, "Action", "Drama", "Fantasy")
        );
    }

    private static SeedAnime tv(String romaji, String english, int episodes, int year, String... genres) {
        return of(romaji, english, AnimeType.TV, AnimeStatus.FINISHED, episodes, year, genres);
    }

    private static SeedAnime movie(String romaji, String english, int year, String... genres) {
        return of(romaji, english, AnimeType.MOVIE, AnimeStatus.FINISHED, 1, year, genres);
    }

    private static SeedAnime ova(String romaji, String english, int episodes, int year, String... genres) {
        return of(romaji, english, AnimeType.OVA, AnimeStatus.FINISHED, episodes, year, genres);
    }

    private static SeedAnime special(String romaji, String english, int episodes, int year, String... genres) {
        return of(romaji, english, AnimeType.SPECIAL, AnimeStatus.FINISHED, episodes, year, genres);
    }

    private static SeedAnime of(
            String titleRomaji,
            String titleEnglish,
            AnimeType type,
            AnimeStatus status,
            int episodeCount,
            int year,
            String... genres
    ) {
        return new SeedAnime(titleRomaji, titleEnglish, type, status, episodeCount, year, genres);
    }

    private record SeedAnime(
            String titleRomaji,
            String titleEnglish,
            AnimeType type,
            AnimeStatus status,
            int episodeCount,
            int year,
            String... genres
    ) {
    }
}
