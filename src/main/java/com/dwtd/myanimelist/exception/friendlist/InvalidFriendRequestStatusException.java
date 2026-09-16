package com.dwtd.myanimelist.exception.friendlist;

import com.dwtd.myanimelist.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidFriendRequestStatusException extends CustomException {
  public InvalidFriendRequestStatusException(String status) {
    super("Friend request is not in the required state: " + status,
            HttpStatus.BAD_REQUEST,
            "INVALID_FRIEND_REQUEST_STATUS");
  }
}