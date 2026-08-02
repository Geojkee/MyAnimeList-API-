package com.dwtd.myanimelist.features.anime.specification;

import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.genre.entity.Genre;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class AnimeSpecification {

    public static Specification<Anime> filterBy(String search, String type, String status, Long genreId) {
        return Specification
                .where(searchLike(search))
                .and(typeEquals(type))
                .and(statusEquals(status))
                .and(hasGenre(genreId));
    }

    public static Specification<Anime> searchLike(String search){
        return (root, query, criteriaBuilder) -> {
            if (!StringUtils.hasText(search)) {
                return criteriaBuilder.conjunction();
            }
            String pattern = "%" + search.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("titleRomaji")), pattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("titleEnglish")), pattern)
            );
        };
    }

    public static Specification<Anime> hasGenre(Long genreId){
        return ((root, query, criteriaBuilder) -> {
           if (genreId == null){
               return criteriaBuilder.conjunction();
           }
            Join<Anime, Genre> genreJoin = root.join("genres");
           return criteriaBuilder.equal(genreJoin.get("id"), genreId);
        });
    }

    private static Specification<Anime> typeEquals(String type) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(type)) {
                return cb.conjunction();
            }
            return cb.equal(root.get("type"), type.toUpperCase());
        };
    }

    private static Specification<Anime> statusEquals(String status) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(status)) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status.toUpperCase());
        };
    }
}
