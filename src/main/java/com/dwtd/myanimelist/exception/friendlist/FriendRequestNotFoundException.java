package com.dwtd.myanimelist.exception.friendlist;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class FriendRequestNotFoundException extends CustomException {
    public FriendRequestNotFoundException(Long requestId) {
        super("Friend request with id " + requestId + " not found",
                HttpStatus.NOT_FOUND,
                "FRIEND_REQUEST_NOT_FOUND");
    }
}