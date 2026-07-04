package com.Csports.Csports.controller;

import com.Csports.Csports.DTO.RegionResponse;
import com.Csports.Csports.service.RegionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/regions")
public class RegionController {

    private final RegionService regionService;

    public RegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    @GetMapping
    public List<RegionResponse> getRegions() {
        return regionService.getAllRegions();
    }
}