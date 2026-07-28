package com.csports.sport;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.csports.sport.dto.SportResponse;
import com.csports.sport.Sport;
import com.csports.sport.SportService;
import com.csports.common.web.ApiPaths;

@RestController
@RequestMapping({ApiPaths.SPORTS, ApiPaths.LEGACY_SPORTS})

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
