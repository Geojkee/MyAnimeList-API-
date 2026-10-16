package com.dwtd.myanimelist.helpers;

import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.anime.enums.AnimeStatus;
import com.dwtd.myanimelist.features.anime.enums.AnimeType;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.comment.entity.Comment;
import com.dwtd.myanimelist.features.friendlist.entity.FriendList;
import com.dwtd.myanimelist.features.friendlist.enums.FriendListStatus;
import com.dwtd.myanimelist.features.genre.entity.Genre;
import com.dwtd.myanimelist.features.tracking.entity.UserAnimeList;
import com.dwtd.myanimelist.features.tracking.enums.UserAnimeStatus;

public final class TestDataFactory {

    private TestDataFactory() {}

    public static User user(String username, String email, Role role) {
        return User.builder()
                .username(username)
                .email(email)
                .password("encoded-password")
                .role(role)
                .build();
    }

    public static Anime anime(String title, int episodeCount) {
        return anime(title, AnimeType.TV, episodeCount, AnimeStatus.FINISHED);
    }

    public static Anime anime(String title, AnimeType type, int episodeCount, AnimeStatus status) {
        return Anime.builder()
                .titleRomaji(title)
                .type(type)
                .episodeCount(episodeCount)
                .status(status)
                .build();
    }

    public static Genre genre(String name) {
        return Genre.builder().name(name).build();
    }

    public static Comment comment(Anime anime, User user, String text) {
        return Comment.builder()
                .anime(anime)
                .user(user)
                .text(text)
                .build();
    }

    public static FriendList friendship(User user, User friend, FriendListStatus status) {
        return FriendList.builder()
                .user(user)
                .friend(friend)
                .status(status)
                .build();
    }

    public static UserAnimeList userAnimeList(
            User user,
            Anime anime,
            UserAnimeStatus status,
            Integer score,
            int watchedEpisodes
    ) {
        return UserAnimeList.builder()
                .user(user)
                .anime(anime)
                .status(status)
                .score(score)
                .watchedEpisodes(watchedEpisodes)
                .build();
    }
}
