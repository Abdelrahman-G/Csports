package com.Csports.Csports.service;

import com.Csports.Csports.DTO.RegionResponse;
import com.Csports.Csports.model.Region;
import com.Csports.Csports.repository.RegionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegionService {

    private final RegionRepository regionRepository;

    public RegionService(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    public List<RegionResponse> getAllRegions() {
        return regionRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    private RegionResponse mapToResponse(Region region) {

        return new RegionResponse(

                region.getId(),

                region.getName(),

                region.getCity(),

                region.getCountry()

        );
    }
}