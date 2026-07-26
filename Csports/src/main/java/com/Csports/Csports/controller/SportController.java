package com.Csports.Csports.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Csports.Csports.DTO.SportResponse;
import com.Csports.Csports.model.Sport;
import com.Csports.Csports.service.SportService;

@RestController
@RequestMapping("/sports")

public class SportController {
    private final SportService sportsService;

    public SportController(SportService sportsService) {
        this.sportsService = sportsService;
    }

    @GetMapping("/list")
    public List<SportResponse> listSports() {
        return sportsService.getSports();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public Sport addSport(@RequestBody Sport sport) {
        return sportsService.addSport(sport);
    }
}
