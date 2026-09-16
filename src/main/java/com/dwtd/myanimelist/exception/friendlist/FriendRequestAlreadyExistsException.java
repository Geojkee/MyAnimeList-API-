package com.dwtd.myanimelist.exception.friendlist;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class FriendRequestAlreadyExistsException extends CustomException {
  public FriendRequestAlreadyExistsException(Long friendId) {
    super("Friend request with user id " + friendId + " already exists",
            HttpStatus.CONFLICT,
            "FRIEND_REQUEST_ALREADY_EXISTS");
  }
}