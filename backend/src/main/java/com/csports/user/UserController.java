package com.csports.user;

import com.csports.booking.dto.BookedSessionResponse;
import com.csports.common.pagination.PageResponse;
import com.csports.common.web.ApiPaths;
import com.csports.user.dto.UpdateUserProfileRequest;
import com.csports.user.dto.UserProfileResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;

@RestController
@RequestMapping({ApiPaths.USERS, ApiPaths.LEGACY_USERS})
@Validated
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public UserProfileResponse getMyProfile() {
        return userService.getMyProfile();
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/me")
    public UserProfileResponse updateMyProfile(
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return userService.updateMyProfile(request);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/sessions")
    public PageResponse<BookedSessionResponse> getMySessions(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {

        return userService.getMySessions(page, size);
    }
}
