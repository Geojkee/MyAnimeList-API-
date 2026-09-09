package com.dwtd.myanimelist.features.comment.repository;

import com.dwtd.myanimelist.features.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByAnimeId(Long animeId, Pageable pageable);
}
