package com.dwtd.myanimelist.exception.friendlist;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class FriendNotFoundException extends CustomException {
    public FriendNotFoundException(Long friendId) {
        super("Friendship with user id " + friendId + " not found",
                HttpStatus.NOT_FOUND,
                "FRIEND_NOT_FOUND");
    }
}