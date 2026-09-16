package com.dwtd.myanimelist.exception.friendlist;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class FriendRequestAccessDeniedException extends CustomException {
    public FriendRequestAccessDeniedException() {
        super("You do not have permission to modify this friend request",
                HttpStatus.FORBIDDEN,
                "FRIEND_REQUEST_ACCESS_DENIED");
    }
}