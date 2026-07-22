package com.dwtd.myanimelist.features.genre.repository;

import com.dwtd.myanimelist.features.genre.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, Long> {

    boolean existsByName(String name);
}
