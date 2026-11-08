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
                .synopsis(row.synopsis())
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
            if (row.synopsis() == null || !row.synopsis().contains(" ")) {
                throw new IllegalStateException("Seed synopsis should be a short description: " + row.titleRomaji());
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
                tv("Cowboy Bebop", "Cowboy Bebop", 26, 1998,
                        "A drifting bounty crew takes jobs across the solar system while old crimes catch up with them.",
                        "Action", "Sci-Fi"),
                tv("Shinseiki Evangelion", "Neon Genesis Evangelion", 26, 1995,
                        "Teenagers are ordered to pilot living machines against beings attacking a ruined Earth.",
                        "Mecha", "Drama", "Sci-Fi"),
                tv("Hagane no Renkinjutsushi", "Fullmetal Alchemist", 51, 2003,
                        "Two brothers who tried to revive their mother search the country for a way to restore their bodies.",
                        "Action", "Adventure", "Fantasy"),
                tv("Hagane no Renkinjutsushi: Fullmetal Alchemist", "Fullmetal Alchemist: Brotherhood", 64, 2009,
                        "The brothers follow the nationwide plot behind the alchemy that cost them their bodies.",
                        "Action", "Adventure", "Fantasy", "Drama"),
                tv("Death Note", "Death Note", 37, 2006,
                        "A student finds a notebook that kills anyone whose name he writes, and a detective starts to close in.",
                        "Mystery", "Supernatural", "Thriller"),
                tv("Steins;Gate", "Steins;Gate", 24, 2011,
                        "A self-styled mad scientist learns his microwave can send words to the past, then cannot undo the damage.",
                        "Sci-Fi", "Thriller"),
                tv("Steins;Gate 0", "Steins;Gate 0", 23, 2018,
                        "In the future where he stopped trying to save her, the same scientist watches the world come apart again.",
                        "Sci-Fi", "Thriller", "Drama"),
                tv("Hunter x Hunter (2011)", "Hunter x Hunter", 148, 2011,
                        "A boy leaves home to become a hunter and to find the father who chose that life over raising him.",
                        "Action", "Adventure"),
                tv("Code Geass: Hangyaku no Lelouch", "Code Geass: Lelouch of the Rebellion", 25, 2006,
                        "An exiled prince gains the power to command anyone and turns it on the empire that took his homeland.",
                        "Action", "Mecha"),
                tv("Code Geass: Hangyaku no Lelouch R2", "Code Geass: Lelouch of the Rebellion R2", 25, 2008,
                        "The prince keeps the rebellion going as both sides learn who he is and the war reaches the capital.",
                        "Action", "Mecha"),
                tv("Tengen Toppa Gurren Lagann", "Gurren Lagann", 27, 2007,
                        "Villagers from underground break to the surface and keep drilling upward through ever larger enemies.",
                        "Action", "Mecha", "Adventure"),
                tv("Samurai Champloo", "Samurai Champloo", 26, 2004,
                        "A waitress hires two rival swordsmen to help her find a samurai who smells of sunflowers.",
                        "Action", "Adventure", "Comedy"),
                tv("Trigun", "Trigun", 26, 1998,
                        "A wandering gunman with a huge bounty tries to settle every town he visits without killing anyone.",
                        "Action", "Sci-Fi", "Comedy"),
                tv("Monster", "Monster", 74, 2004,
                        "A doctor saves a boy who grows up to be a murderer, then crosses Europe to stop him.",
                        "Mystery", "Thriller", "Drama"),
                tv("Kenpuu Denki Berserk", "Berserk", 25, 1997,
                        "A swordsman joins a mercenary band whose captain is climbing toward a fate that is no longer human.",
                        "Action", "Fantasy", "Horror"),
                tv("Koukaku Kidoutai: Stand Alone Complex", "Ghost in the Shell: Stand Alone Complex", 26, 2002,
                        "A cyborg police team in a networked city hunts hackers who can hide inside other people's minds.",
                        "Sci-Fi", "Action", "Mystery"),
                tv("Psycho-Pass", "Psycho-Pass", 22, 2012,
                        "Inspectors arrest people by a mental scan, until one of them stops trusting the system that gives the verdict.",
                        "Sci-Fi", "Thriller", "Action"),
                tv("Ergo Proxy", "Ergo Proxy", 23, 2006,
                        "In a domed city run by robots, an investigator follows androids that have started to think for themselves.",
                        "Sci-Fi", "Mystery"),
                tv("Serial Experiments Lain", "Serial Experiments Lain", 13, 1998,
                        "A quiet girl is drawn into the Wired after a classmate's death, and her sense of self begins to split.",
                        "Sci-Fi", "Mystery", "Drama"),
                tv("Mou Sou Dairinin", "Paranoia Agent", 13, 2004,
                        "A detective follows attacks by a boy with a golden bat, while the story spreads through the city like a rumor.",
                        "Mystery", "Horror", "Thriller"),
                tv("Haibane Renmei", "Haibane Renmei", 13, 2002,
                        "People with halos and small wings wake in a walled town and cannot remember the life they left.",
                        "Fantasy", "Drama", "Mystery"),
                tv("Mushishi", "Mushishi", 26, 2005,
                        "A traveler who studies spirit-like mushi walks from village to village, easing the trouble they cause.",
                        "Fantasy", "Slice of Life", "Supernatural"),
                tv("Natsume Yuujinchou", "Natsume's Book of Friends", 13, 2008,
                        "A boy who sees spirits inherits a book of their names and tries to give those names back.",
                        "Slice of Life", "Supernatural"),
                tv("Barakamon", "Barakamon", 12, 2014,
                        "A calligrapher sent to a remote island relearns his brush from the children who keep invading his house.",
                        "Slice of Life", "Comedy"),
                tv("3-gatsu no Lion", "March Comes in Like a Lion", 22, 2016,
                        "A young shogi pro and a lonely student cross paths while each tries to rebuild a life around the game.",
                        "Drama", "Slice of Life"),
                tv("Clannad", "Clannad", 23, 2007,
                        "A delinquent starts helping classmates in a town where small family troubles attract something like a miracle.",
                        "Drama", "Romance", "Supernatural"),
                tv("Clannad: After Story", "Clannad: After Story", 24, 2008,
                        "The same couple steps into work, marriage, and the weight of the promise the town once made them.",
                        "Drama", "Romance", "Supernatural"),
                tv("Toradora!", "Toradora!", 25, 2008,
                        "Two short-tempered classmates agree to help each other win someone else, then cannot stay out of each other's way.",
                        "Romance", "Comedy", "Slice of Life"),
                tv("Ano Hi Mita Hana no Namae wo Bokutachi wa Mada Shiranai.", "Anohana: The Flower We Saw That Day", 11, 2011,
                        "Childhood friends drift back together when the girl who died years ago appears again to only one of them.",
                        "Drama", "Supernatural"),
                tv("Shigatsu wa Kimi no Uso", "Your Lie in April", 22, 2014,
                        "A pianist who can no longer bear his own playing meets a violinist who pulls him back onto the stage.",
                        "Drama", "Romance", "Music"),
                tv("Violet Evergarden", "Violet Evergarden", 13, 2018,
                        "A former child soldier becomes a letter writer, trying to understand the last words her commander left her.",
                        "Drama", "Fantasy"),
                tv("Sora yori mo Tooi Basho", "A Place Further Than the Universe", 13, 2018,
                        "Four high-school girls scrape together an unapproved trip to Antarctica.",
                        "Adventure", "Drama"),
                tv("Made in Abyss", "Made in Abyss", 13, 2017,
                        "A girl climbs down a pit full of monsters to look for her mother, knowing almost nobody comes back up.",
                        "Adventure", "Fantasy", "Mystery"),
                tv("Odd Taxi", "Odd Taxi", 13, 2021,
                        "A walrus taxi driver listens to passengers whose separate stories all point at one missing girl.",
                        "Mystery", "Comedy"),
                tv("Sousou no Frieren", "Frieren: Beyond Journey's End", 28, 2023,
                        "An elf mage who outlived her heroic party takes an apprentice and tries to understand the people she traveled with.",
                        "Adventure", "Fantasy", "Drama"),
                tv("Bocchi the Rock!", "Bocchi the Rock!", 12, 2022,
                        "A girl who can barely speak to strangers becomes the guitarist a new band needs on stage.",
                        "Comedy", "Music", "Slice of Life"),
                tv("K-On!", "K-On!", 13, 2009,
                        "A high-school music club stays alive by recruiting whoever walks in and practicing just enough to keep the room.",
                        "Comedy", "Music", "Slice of Life"),
                tv("Hibike! Euphonium", "Sound! Euphonium", 13, 2015,
                        "A school concert band that used to settle for average decides it wants to reach the national competition.",
                        "Drama", "Music"),
                tv("Mob Psycho 100", "Mob Psycho 100", 12, 2016,
                        "A quiet boy with overwhelming psychic power tries to stay ordinary while spirits and con men keep finding him.",
                        "Action", "Comedy", "Supernatural"),
                tv("One Punch Man", "One Punch Man", 12, 2015,
                        "A hero who can end any fight with one punch goes looking for an opponent that still feels like a challenge.",
                        "Action", "Comedy"),
                tv("Vinland Saga", "Vinland Saga", 24, 2019,
                        "A boy raised among Viking warriors sets out to kill the man who took his father.",
                        "Action", "Adventure", "Drama"),
                tv("Shingeki no Kyojin", "Attack on Titan", 25, 2013,
                        "People live behind walls because giants eat anyone caught outside, until one of those walls is broken.",
                        "Action", "Drama", "Fantasy"),
                tv("Kimetsu no Yaiba", "Demon Slayer: Kimetsu no Yaiba", 26, 2019,
                        "After a demon kills his family and curses his sister, a boy joins the corps that hunts them.",
                        "Action", "Fantasy", "Supernatural"),
                tv("Jujutsu Kaisen", "Jujutsu Kaisen", 24, 2020,
                        "A student swallows a cursed finger and is sent to a school that exorcises the curses born from fear.",
                        "Action", "Supernatural"),
                tv("Chainsaw Man", "Chainsaw Man", 12, 2022,
                        "A broke devil hunter merges with a chainsaw devil and is recruited by a bureau that wants to own him.",
                        "Action", "Horror", "Supernatural"),
                tv("Boku no Hero Academia", "My Hero Academia", 13, 2016,
                        "In a world where almost everyone has a power, a boy born without one inherits the strongest of them.",
                        "Action"),
                tv("Kaguya-sama wa Kokurasetai: Tensai-tachi no Renai Zunousen", "Kaguya-sama: Love Is War", 12, 2019,
                        "The student-council president and vice president plot to make the other confess their love first.",
                        "Comedy", "Romance"),
                tv("Re:Zero kara Hajimeru Isekai Seikatsu", "Re:Zero - Starting Life in Another World", 25, 2016,
                        "A boy pulled into another world finds that death sends him back, and that nobody else remembers the loop.",
                        "Fantasy", "Thriller", "Drama"),
                tv("Kono Subarashii Sekai ni Shukufuku wo!", "KonoSuba: God's Blessing on This Wonderful World!", 10, 2016,
                        "A shut-in is reborn in a fantasy world with a useless goddess and a party that cannot stop arguing.",
                        "Comedy", "Fantasy", "Adventure"),
                tv("Dungeon Meshi", "Delicious in Dungeon", 24, 2024,
                        "Adventurers cook the monsters they defeat while climbing down a dungeon to save a swallowed friend.",
                        "Adventure", "Fantasy", "Comedy"),
                tv("Cyberpunk: Edgerunners", "Cyberpunk: Edgerunners", 10, 2022,
                        "A street kid in Night City installs illegal chrome and joins mercenaries who burn out young.",
                        "Action", "Sci-Fi"),
                tv("Baccano!", "Baccano!", 13, 2007,
                        "Immortal gangsters, thieves, and a massacre on a cross-country train are told out of order until the stories meet.",
                        "Action", "Mystery"),
                tv("Durarara!!", "Durarara!!", 24, 2010,
                        "Gangs, a headless rider, and an information broker collide in Ikebukuro because everyone is chasing a rumor.",
                        "Action", "Mystery", "Supernatural"),
                tv("Bungou Stray Dogs", "Bungo Stray Dogs", 12, 2016,
                        "People named after famous authors fight with powers drawn from books, and a small agency tries to keep the city standing.",
                        "Action", "Mystery", "Supernatural"),
                tv("Noragami", "Noragami", 12, 2014,
                        "A minor god without a shrine takes odd jobs for worshippers, helped by a girl who can see the spirit world.",
                        "Action", "Supernatural"),
                tv("Ping Pong the Animation", "Ping Pong the Animation", 11, 2014,
                        "Two childhood friends take table tennis in opposite directions, and the game stops being only a game.",
                        "Sports", "Drama"),
                tv("Haikyuu!!", "Haikyu!!", 25, 2014,
                        "A short boy joins a high-school volleyball team determined to make his height an advantage at the net.",
                        "Sports", "Comedy"),
                tv("Kaze ga Tsuyoku Fuiteiru", "Run with the Wind", 23, 2018,
                        "A former runner talks the residents of a run-down dorm into entering the Hakone relay.",
                        "Sports", "Drama"),
                tv("Hajime no Ippo", "Fighting Spirit", 75, 2000,
                        "A bullied boy finds he can box and starts climbing out of a neighborhood gym.",
                        "Sports", "Drama"),
                tv("Slam Dunk", "Slam Dunk", 101, 1993,
                        "A delinquent joins the basketball team to impress a girl and discovers he cannot walk away from the sport.",
                        "Sports", "Comedy"),
                tv("Initial D First Stage", "Initial D First Stage", 26, 1998,
                        "A tofu-shop delivery driver who learned the mountain passes is pulled into late-night street races.",
                        "Action", "Sports"),
                tv("Planetes", "Planetes", 26, 2003,
                        "Debris collectors in Earth orbit clear junk before it kills the people building a mission farther out.",
                        "Sci-Fi", "Drama"),
                tv("Nichijou", "Nichijou - My Ordinary Life", 26, 2011,
                        "Ordinary school days keep derailing, partly because one of the students lives with a robot girl.",
                        "Comedy", "Slice of Life"),
                tv("Hyouka", "Hyouka", 22, 2012,
                        "A boy who wants a quiet school life is dragged into small mysteries by a classmate who cannot leave them alone.",
                        "Mystery", "Slice of Life"),
                tv("Nodame Cantabile", "Nodame Cantabile", 23, 2007,
                        "A strict pianist and a chaotic one are pushed together at a music college and somehow start an orchestra.",
                        "Comedy", "Romance", "Music"),
                tv("Sakamichi no Apollon", "Kids on the Slope", 12, 2012,
                        "In the 1960s a transfer student finds jazz, and a drummer, on the slope home from school.",
                        "Drama", "Music", "Romance"),
                tv("Boku dake ga Inai Machi", "Erased", 12, 2016,
                        "A man who can slip minutes into the past is thrown years back, to the month a classmate was killed.",
                        "Mystery", "Thriller"),
                tv("Zankyou no Terror", "Terror in Resonance", 11, 2014,
                        "Two boys steal an atomic weapon and turn Tokyo into a riddle the police have to answer.",
                        "Mystery", "Thriller"),
                tv("Shinsekai yori", "From the New World", 25, 2012,
                        "Children in a village of psychics slowly learn what their society does to anyone who breaks its rules.",
                        "Drama", "Mystery", "Sci-Fi", "Horror"),
                tv("Kiseijuu: Sei no Kakuritsu", "Parasyte: The Maxim", 24, 2014,
                        "An alien parasite misses a student's brain and takes his right hand, while others finish the job on everyone else.",
                        "Action", "Horror", "Sci-Fi"),
                tv("Kidou Senshi Gundam", "Mobile Suit Gundam", 43, 1979,
                        "A civilian boy falls into the cockpit of a secret mobile suit during a war between Earth and its colonies.",
                        "Mecha", "Action", "Drama"),
                tv("Tenkuu no Escaflowne", "The Vision of Escaflowne", 26, 1996,
                        "A girl is pulled into a world at war, where a prince fights in armor that is also a dragon.",
                        "Adventure", "Fantasy", "Mecha", "Romance"),
                tv("Koukyoushihen Eureka Seven", "Eureka Seven", 50, 2005,
                        "A boy runs off with a renegade crew and the pilot of a robot he does not yet understand.",
                        "Mecha", "Adventure", "Romance"),
                tv("Darker than Black: Kuro no Keiyakusha", "Darker than Black", 25, 2007,
                        "Agents with powers pay a personal price for every use, and a fixer in Tokyo takes the jobs nobody signs.",
                        "Action", "Mystery", "Sci-Fi"),
                tv("Soul Eater", "Soul Eater", 51, 2008,
                        "Students training as weapons and their partners hunt tainted souls before a witch can collect them.",
                        "Action", "Fantasy", "Comedy"),
                tv("Yu Yu Hakusho", "Yu Yu Hakusho", 112, 1992,
                        "A street fighter dies saving a child and is hired back as a detective for the spirit world.",
                        "Action", "Supernatural"),
                tv("Rurouni Kenshin: Meiji Kenkaku Romantan", "Rurouni Kenshin", 94, 1996,
                        "A former assassin wanders the new Meiji era with a reversed blade, trying never to kill again.",
                        "Action", "Adventure", "Romance"),
                tv("Great Pretender", "Great Pretender", 23, 2020,
                        "Con artists take down wealthy marks, and each job is less clean than the one before it.",
                        "Action", "Comedy", "Thriller"),
                tv("Death Parade", "Death Parade", 12, 2015,
                        "The recently dead play games in a bar so the bartender can judge what should happen to their souls.",
                        "Drama", "Mystery"),
                tv("Chihayafuru", "Chihayafuru", 25, 2011,
                        "A girl rebuilds a competitive team around karuta, the poem-card game she refused to give up.",
                        "Sports", "Drama"),
                movie("Sen to Chihiro no Kamikakushi", "Spirited Away", 2001,
                        "A girl trapped in a spirit bathhouse has to work there until she can get her parents back.",
                        "Fantasy", "Adventure"),
                movie("Mononoke-hime", "Princess Mononoke", 1997,
                        "A warrior raised by wolves is caught between the forest gods and the iron town cutting the forest down.",
                        "Fantasy", "Adventure", "Action"),
                movie("Tonari no Totoro", "My Neighbor Totoro", 1988,
                        "Two sisters in the countryside meet a forest spirit while they wait for their mother to come home from the hospital.",
                        "Fantasy", "Slice of Life"),
                movie("Kimi no Na wa.", "Your Name.", 2016,
                        "A boy and a girl start waking up in each other's bodies and try to find one another.",
                        "Romance", "Drama", "Supernatural"),
                movie("Koe no Katachi", "A Silent Voice", 2016,
                        "A boy who once bullied a deaf classmate spends his teens trying to face her and make amends.",
                        "Drama"),
                movie("Howl no Ugoku Shiro", "Howl's Moving Castle", 2004,
                        "A girl cursed into old age looks for the wizard Howl, whose moving castle is also his hiding place from a war.",
                        "Fantasy", "Adventure", "Romance"),
                movie("Hotaru no Haka", "Grave of the Fireflies", 1988,
                        "A brother and his little sister try to survive on their own in the last months of the war.",
                        "Drama"),
                movie("Akira", "Akira", 1988,
                        "A biker in Neo-Tokyo crashes into a military secret and a child whose power the city cannot hold.",
                        "Action", "Sci-Fi"),
                movie("Koukaku Kidoutai", "Ghost in the Shell", 1995,
                        "A cyborg officer hunts a hacker who can rewrite a person's memories, including her own.",
                        "Sci-Fi", "Action"),
                movie("Perfect Blue", "Perfect Blue", 1997,
                        "A pop idol turns to acting and can no longer separate her stalker, her roles, and her own mind.",
                        "Thriller", "Drama", "Mystery"),
                movie("Toki wo Kakeru Shoujo", "The Girl Who Leapt Through Time", 2006,
                        "A schoolgirl gains the power to jump backward in time and learns that every leap costs her something.",
                        "Sci-Fi", "Romance"),
                movie("Ookami Kodomo no Ame to Yuki", "Wolf Children", 2012,
                        "After their father dies, a woman raises two children who can turn into wolves, with no one she can tell.",
                        "Fantasy", "Slice of Life", "Drama"),
                movie("Tenki no Ko", "Weathering with You", 2019,
                        "A runaway boy meets a girl who can clear the sky, and her sunshine is not free.",
                        "Romance", "Fantasy"),
                movie("Suzume no Tojimari", "Suzume", 2022,
                        "A girl helps a stranger close doors that let disasters through, while he is stuck in the shape of a chair.",
                        "Adventure", "Fantasy"),
                movie("Kimetsu no Yaiba: Mugen Ressha-hen", "Demon Slayer: Mugen Train", 2020,
                        "Demon slayers board a train where a demon keeps the passengers inside dreams they do not want to leave.",
                        "Action", "Fantasy", "Supernatural"),
                ova("FLCL", "FLCL", 6, 2000,
                        "A woman on a scooter hits a boy with a guitar, and robots begin growing out of his head.",
                        "Action", "Comedy", "Sci-Fi"),
                ova("Hellsing Ultimate", "Hellsing Ultimate", 10, 2006,
                        "A vampire bound to the British crown is sent to destroy a remnant army that raises its own dead.",
                        "Action", "Horror", "Supernatural"),
                ova("Top wo Nerae! Gunbuster", "Gunbuster", 6, 1988,
                        "A trainee pilot is thrown into a war against space monsters, the same war that killed her father.",
                        "Mecha", "Sci-Fi"),
                ova("Kidou Senshi Gundam 0080: Pocket no Naka no Sensou", "Mobile Suit Gundam 0080: War in the Pocket", 6, 1989,
                        "A boy in a neutral colony befriends the enemy pilot hiding next door during a secret raid.",
                        "Mecha", "Drama"),
                special("Shingeki no Kyojin: The Final Chapters", "Attack on Titan: The Final Chapters", 2, 2023,
                        "The survivors of the war inside the walls reach the battle that decides what is left of the world.",
                        "Action", "Drama", "Fantasy")
        );
    }

    private static SeedAnime tv(
            String romaji,
            String english,
            int episodes,
            int year,
            String synopsis,
            String... genres
    ) {
        return of(romaji, english, AnimeType.TV, AnimeStatus.FINISHED, episodes, year, synopsis, genres);
    }

    private static SeedAnime movie(String romaji, String english, int year, String synopsis, String... genres) {
        return of(romaji, english, AnimeType.MOVIE, AnimeStatus.FINISHED, 1, year, synopsis, genres);
    }

    private static SeedAnime ova(
            String romaji,
            String english,
            int episodes,
            int year,
            String synopsis,
            String... genres
    ) {
        return of(romaji, english, AnimeType.OVA, AnimeStatus.FINISHED, episodes, year, synopsis, genres);
    }

    private static SeedAnime special(
            String romaji,
            String english,
            int episodes,
            int year,
            String synopsis,
            String... genres
    ) {
        return of(romaji, english, AnimeType.SPECIAL, AnimeStatus.FINISHED, episodes, year, synopsis, genres);
    }

    private static SeedAnime of(
            String titleRomaji,
            String titleEnglish,
            AnimeType type,
            AnimeStatus status,
            int episodeCount,
            int year,
            String synopsis,
            String... genres
    ) {
        return new SeedAnime(titleRomaji, titleEnglish, type, status, episodeCount, year, synopsis, genres);
    }

    private record SeedAnime(
            String titleRomaji,
            String titleEnglish,
            AnimeType type,
            AnimeStatus status,
            int episodeCount,
            int year,
            String synopsis,
            String... genres
    ) {
    }
}
