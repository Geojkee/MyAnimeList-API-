package com.dwtd.myanimelist.features.anime.specification;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;

class AnimeSpecificationTest {

    @Test
    void filterBy_shouldBuildSpecification() {
        assertThat((Specification<?>) AnimeSpecification.filterBy(null, null, null, null)).isNotNull();
        assertThat((Specification<?>) AnimeSpecification.searchLike(null)).isNotNull();
        assertThat((Specification<?>) AnimeSpecification.hasGenre(null)).isNotNull();
        assertThat(new AnimeSpecification()).isNotNull();
    }
}
