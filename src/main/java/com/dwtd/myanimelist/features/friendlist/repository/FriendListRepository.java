package com.dwtd.myanimelist.features.friendlist.repository;

import com.dwtd.myanimelist.features.friendlist.entity.FriendList;
import com.dwtd.myanimelist.features.friendlist.enums.FriendListStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendListRepository extends JpaRepository<FriendList, Long> {

    Optional<FriendList> findByUserIdAndFriendId(Long userId, Long friendId);

    @Query("SELECT f FROM FriendList f WHERE " +
            "(f.user.id = :userId AND f.friend.id = :friendId) OR " +
            "(f.user.id = :friendId AND f.friend.id = :userId)")
    Optional<FriendList> findBetweenUsers(@Param("userId") Long userId,
                                          @Param("friendId") Long friendId);

    boolean existsByUserIdAndFriendId(Long userId, Long friendId);

    @Query("SELECT f FROM FriendList f WHERE " +
            "(f.user.id = :userId OR f.friend.id = :userId) " +
            "AND f.status = :status")
    List<FriendList> findAllFriendsByUserIdAndStatus(@Param("userId") Long userId,
                                                     @Param("status") FriendListStatus status);

    List<FriendList> findAllByFriendIdAndStatus(Long friendId, FriendListStatus status);

    List<FriendList> findAllByUserIdAndStatus(Long userId, FriendListStatus status);

    @Modifying
    @Query("DELETE FROM FriendList f WHERE " +
            "(f.user.id = :userId AND f.friend.id = :friendId) OR " +
            "(f.user.id = :friendId AND f.friend.id = :userId)")
    void deleteBetweenUsers(@Param("userId") Long userId,
                            @Param("friendId") Long friendId);
}
