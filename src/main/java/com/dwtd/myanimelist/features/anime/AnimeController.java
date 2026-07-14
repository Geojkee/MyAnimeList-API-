package com.dwtd.myanimelist.features.anime;

import com.dwtd.myanimelist.features.anime.dto.AnimeRequest;
import com.dwtd.myanimelist.features.anime.dto.AnimeResponse;
import com.dwtd.myanimelist.features.anime.service.AnimeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/anime")
@Tag(name = "Anime")
public class AnimeController {

    private final AnimeService animeService;

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AnimeResponse> create(@RequestBody @Valid AnimeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(animeService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnimeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(animeService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AnimeResponse> update(@PathVariable Long id, @RequestBody @Valid AnimeRequest request) {
        return ResponseEntity.ok(animeService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        animeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
