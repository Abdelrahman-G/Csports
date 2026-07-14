package com.Csports.Csports.controller;
import java.util.List;

import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.Csports.Csports.model.Sport;
import com.Csports.Csports.repository.SportRepository;
import com.Csports.Csports.service.SportService;

@RestController
@RequestMapping("/sports")
public class SportController {
    private final SportService sportsService;

    public SportController(SportService sportsService) {
        this.sportsService = sportsService;
    }
    @GetMapping("/list")
    public List<Sport> listSports() {
        return sportsService.getSports();
    }
    
    @PostAuthorize("hasRole('ADMIN')")
    @PostMapping("/add")
    public Sport addSport(@RequestBody Sport sport) {
        return sportsService.addSport(sport);
    }
}
