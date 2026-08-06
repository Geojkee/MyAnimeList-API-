package com.dwtd.myanimelist.features.tracking;

import com.dwtd.myanimelist.features.auth.service.UserService;
import com.dwtd.myanimelist.features.tracking.dto.UserAnimeListRequest;
import com.dwtd.myanimelist.features.tracking.dto.UserAnimeListResponse;
import com.dwtd.myanimelist.features.tracking.dto.UserAnimeListSummary;
import com.dwtd.myanimelist.features.tracking.service.UserAnimeListService;
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
public class UserAnimeListController {

    private final UserAnimeListService userAnimeListService;
    private final UserService userService;

    @PostMapping("/me/animelist")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserAnimeListResponse> addOrUpdate(
            @RequestBody @Valid UserAnimeListRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(userAnimeListService.addOrUpdate(request));
    }

    @GetMapping("/{username}/animelist")
    public ResponseEntity<List<UserAnimeListSummary>> getUserList(
            @PathVariable String username,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(userAnimeListService.getUserList(username, status));
    }

    @DeleteMapping("/me/animelist/{animeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> remove(
            @PathVariable Long animeId
    ) {
        Long userId = userService.getCurrentUser().getId();
        userAnimeListService.remove(userId, animeId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}