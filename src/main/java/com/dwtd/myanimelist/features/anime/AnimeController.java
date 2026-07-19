package com.dwtd.myanimelist.features.anime;

import com.dwtd.myanimelist.features.anime.dto.AnimeRequest;
import com.dwtd.myanimelist.features.anime.dto.AnimeResponse;
import com.dwtd.myanimelist.features.anime.entity.Anime;
import com.dwtd.myanimelist.features.anime.service.AnimeService;
import com.dwtd.myanimelist.features.anime.specification.AnimeSpecification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
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

    @Operation(summary = "Create anime")
    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AnimeResponse> create(@RequestBody @Valid AnimeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(animeService.create(request));
    }

    @Operation(summary = "Show all anime")
    @GetMapping()
    public ResponseEntity<Page<AnimeResponse>> getAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ){
        Specification<Anime> spec = AnimeSpecification.filterBy(search, type, status);
        Page<AnimeResponse> page = animeService.findAll(spec, pageable);

        return ResponseEntity.status(HttpStatus.OK).body(page);
    }

    @Operation(summary = "Show anime by id")
    @GetMapping("/{id}")
    public ResponseEntity<AnimeResponse> getById(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(animeService.findById(id));
    }

    @Operation(summary = "Update anime by id")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<AnimeResponse> update(@PathVariable Long id, @RequestBody @Valid AnimeRequest request) {
        return ResponseEntity.ok(animeService.update(id, request));
    }

    @Operation(summary = "Delete anime by id")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        animeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}