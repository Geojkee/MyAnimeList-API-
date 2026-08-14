package com.dwtd.myanimelist.features.tracking;

import com.dwtd.myanimelist.features.tracking.dto.AddUserAnimeListRequest;
import com.dwtd.myanimelist.features.tracking.dto.UpdateUserAnimeListRequest;
import com.dwtd.myanimelist.features.tracking.dto.UserAnimeListResponse;
import com.dwtd.myanimelist.features.tracking.dto.UserAnimeListSummary;
import com.dwtd.myanimelist.features.tracking.service.UserAnimeListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Anime List")
public class UserAnimeListController {

    private final UserAnimeListService userAnimeListService;

    @Operation(summary = "Add anime in users list by ID")
    @PostMapping("/me/animelist")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserAnimeListResponse> addAnimeToList(
            @RequestBody @Valid AddUserAnimeListRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userAnimeListService.addAnimeToList(request));
    }

    @Operation(summary = "Show user anime list by username")
    @GetMapping("/{username}/animelist")
    public ResponseEntity<List<UserAnimeListSummary>> getUserList(
            @PathVariable String username,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(userAnimeListService.getUserList(username, status));
    }

    @Operation(summary = "Update anime in user anime list by anime ID")
    @PatchMapping("/me/animelist/{animeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserAnimeListResponse> updateAnimeInList(
            @PathVariable Long animeId,
            @RequestBody @Valid UpdateUserAnimeListRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(userAnimeListService.updateAnimeInList(animeId, request));
    }

    @Operation(summary = "Delete anime in user anime list by ID")
    @DeleteMapping("/me/animelist/{animeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> delete(
            @PathVariable Long animeId
    ) {
        userAnimeListService.delete(animeId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}