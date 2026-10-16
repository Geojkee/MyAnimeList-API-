package com.dwtd.myanimelist.features.user;

import com.dwtd.myanimelist.features.user.dto.UpdateUserProfileRequest;
import com.dwtd.myanimelist.features.user.dto.UserProfileResponse;
import com.dwtd.myanimelist.features.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "User Profile")
public class UserController {

    private final UserProfileService userProfileService;

    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> getCurrentProfile() {
        return ResponseEntity.status(HttpStatus.OK).body(userProfileService.getCurrentProfile());
    }

    @Operation(summary = "Update current user profile")
    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateCurrentProfile(
            @RequestBody @Valid UpdateUserProfileRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK).body(userProfileService.updateCurrentProfile(request));
    }

    @Operation(summary = "Get public user profile by username")
    @GetMapping("/{username}")
    public ResponseEntity<UserProfileResponse> getPublicProfile(@PathVariable String username) {
        return ResponseEntity.status(HttpStatus.OK).body(userProfileService.getPublicProfile(username));
    }
}
