package com.csports.location;

import com.csports.common.web.ApiPaths;
import com.csports.location.dto.RegionResponse;
import com.csports.location.RegionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({ApiPaths.REGIONS, ApiPaths.LEGACY_REGIONS})
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
