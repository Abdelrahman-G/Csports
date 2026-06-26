package com.Csports.Csports.controller;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.Csports.Csports.model.Sport;
import com.Csports.Csports.repository.SportRepository;

@RestController
@RequestMapping("/sports")
public class SportController {
    private final SportRepository sportsRepo;

    public SportController(SportRepository sportsRepo) {
        this.sportsRepo = sportsRepo;
    }
    @GetMapping("/list")
    public List<Sport> listSports() {
        return sportsRepo.findAll();
    }
}
