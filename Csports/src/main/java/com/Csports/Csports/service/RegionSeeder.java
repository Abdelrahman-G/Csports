package com.Csports.Csports.service;

import com.Csports.Csports.model.Region;
import com.Csports.Csports.repository.RegionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RegionSeeder implements CommandLineRunner {

        private final RegionRepository regionRepository;

        public RegionSeeder(RegionRepository regionRepository) {
                this.regionRepository = regionRepository;
        }

        @Override
        public void run(String... args) {

                if (regionRepository.count() > 0) {
                        return;
                }

                List<Region> regions = List.of(

                                Region.builder()
                                                .country("Egypt")
                                                .city("Cairo")
                                                .name("Nasr City")
                                                .latitude(30.0581)
                                                .longitude(31.3302)
                                                .build(),

                                Region.builder()
                                                .country("Egypt")
                                                .city("Cairo")
                                                .name("Maadi")
                                                .latitude(29.9602)
                                                .longitude(31.2569)
                                                .build(),

                                Region.builder()
                                                .country("Egypt")
                                                .city("Cairo")
                                                .name("Heliopolis")
                                                .latitude(30.0910)
                                                .longitude(31.3260)
                                                .build(),

                                Region.builder()
                                                .country("Egypt")
                                                .city("Cairo")
                                                .name("New Cairo")
                                                .latitude(30.0285)
                                                .longitude(31.4913)
                                                .build(),

                                Region.builder()
                                                .country("Egypt")
                                                .city("Giza")
                                                .name("6th of October")
                                                .latitude(29.9765)
                                                .longitude(30.9445)
                                                .build(),
                                Region.builder()
                                                .country("Egypt")
                                                .city("Giza")
                                                .name("Hadayek al Ahram")
                                                .latitude(29.9681637)
                                                .longitude(31.0795433)
                                                .build());

                regionRepository.saveAll(regions);
        }
}