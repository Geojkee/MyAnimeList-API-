package com.dwtd.myanimelist.features.anime.repository;

import com.dwtd.myanimelist.features.anime.entity.Anime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AnimeRepository extends JpaRepository<Anime, Long>, JpaSpecificationExecutor<Anime>  {

    boolean existsByTitleRomaji(String titleRomaji);

    boolean existsByTitleRomajiAndIdNot(String titleRomaji, Long id);
}
