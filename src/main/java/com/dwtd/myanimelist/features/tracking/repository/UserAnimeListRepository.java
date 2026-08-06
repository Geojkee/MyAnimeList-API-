package com.dwtd.myanimelist.features.tracking.repository;

import com.dwtd.myanimelist.features.tracking.entity.UserAnimeList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAnimeListRepository extends JpaRepository<UserAnimeList, Long> {

    Optional<UserAnimeList> findByUserIdAndAnimeId(Long userId, Long animeId);

    List<UserAnimeList> findByUserId(Long userId);

    boolean existsByUserIdAndAnimeId(Long userId, Long animeId);

    @Modifying
    @Query("DELETE FROM UserAnimeList u WHERE u.user.id = :userId AND u.anime.id = :animeId")
    void deleteByUserIdAndAnimeId(@Param("userId") Long userId, @Param("animeId") Long animeId);
}
