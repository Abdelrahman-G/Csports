package com.Csports.Csports.controller;

import com.Csports.Csports.DTO.BookedSessionResponse;
import com.Csports.Csports.DTO.PageResponse;
import com.Csports.Csports.service.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
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