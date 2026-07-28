package com.csports.user;

import com.csports.booking.dto.BookedSessionResponse;
import com.csports.common.pagination.PageResponse;
import com.csports.common.web.ApiPaths;
import com.csports.user.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({ApiPaths.USERS, ApiPaths.LEGACY_USERS})
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/sessions")
    public PageResponse<BookedSessionResponse> getMySessions(@RequestParam(defaultValue = "0")int page, @RequestParam(defaultValue = "10")int size) {

        return userService.getMySessions(page, size);
    }
}
