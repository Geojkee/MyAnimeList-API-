package com.dwtd.myanimelist.features.anime.specification;

import com.dwtd.myanimelist.features.anime.entity.Anime;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class AnimeSpecification {

    public static Specification<Anime> filterBy(String search, String type, String status) {
        return Specification
                .where(searchLike(search))
                .and(typeEquals(type))
                .and(statusEquals(status));
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
