package com.dwtd.myanimelist.features.genre.entity;

import com.dwtd.myanimelist.features.anime.entity.Anime;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Table(name = "genres")
@Builder
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @ManyToMany(mappedBy = "genres")
    private Set<Anime> anime;
}
