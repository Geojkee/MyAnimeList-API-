package com.dwtd.myanimelist.helpers;

import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.anime.enums.AnimeStatus;
import com.dwtd.myanimelist.features.anime.enums.AnimeType;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.auth.enums.Role;
import com.dwtd.myanimelist.features.friendlist.entity.FriendList;
import com.dwtd.myanimelist.features.friendlist.enums.FriendListStatus;

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
        return Anime.builder()
                .titleRomaji(title)
                .type(AnimeType.TV)
                .episodeCount(episodeCount)
                .status(AnimeStatus.FINISHED)
                .build();
    }

    public static FriendList friendship(User user, User friend, FriendListStatus status) {
        return FriendList.builder()
                .user(user)
                .friend(friend)
                .status(status)
                .build();
    }
}