package com.dwtd.myanimelist.features.tracking.entity;

import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.auth.entity.User;
import com.dwtd.myanimelist.features.tracking.enums.UserAnimeStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Builder
@Entity
@Table(name = "user_anime_list")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserAnimeList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserAnimeStatus status;

    @Min(value = 1, message = "Score must be between 1 and 10")
    @Max(value = 10, message = "Score must be between 1 and 10")
    @Column(name = "score")
    private Integer score;

    @Min(value = 0, message = "Watched episodes cannot be negative")
    @Column(name = "watched_episodes", nullable = false)
    private Integer watchedEpisodes = 0;

    @CreationTimestamp
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    @Column(name = "created_at", updatable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
    private Instant updatedAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anime_id", nullable = false)
    private Anime anime;
}