package com.Csports.Csports.service;

import java.time.Duration;
import java.util.List;

import org.springframework.stereotype.Service;

import com.Csports.Csports.DTO.SportResponse;
import com.Csports.Csports.model.Sport;
import com.Csports.Csports.repository.SportRepository;

import tools.jackson.core.type.TypeReference;

@Service
public class SportService {
    private final SportRepository sportRepository;
    private final RedisService redisService;
    private static final String SPORTS_CACHE_KEY = "sports";

    public SportService(SportRepository sportRepository, RedisService redisService) {
        this.sportRepository = sportRepository;
        this.redisService = redisService;
    }

    public List<SportResponse> getSports() {
        List<SportResponse> cachedSports = redisService.get(SPORTS_CACHE_KEY, new TypeReference<List<SportResponse>>() {});

        if (cachedSports != null) {
            System.out.println("Returning sports from Redis");
            return cachedSports;
        }

        List<SportResponse> sports = sportRepository.findAll()
                .stream()
                .map(sport -> new SportResponse(
                        sport.getId(),
                        sport.getName()))
                .toList();

        redisService.save(
                SPORTS_CACHE_KEY,
                sports,
                Duration.ofHours(24));

        return sports;
    }

    public Sport addSport(Sport sport) {
        Sport savedSport = sportRepository.save(sport);
        redisService.delete(SPORTS_CACHE_KEY);
        return savedSport;
    }
}