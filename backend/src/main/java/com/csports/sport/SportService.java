package com.csports.sport;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.csports.sport.dto.SportResponse;
import com.csports.infrastructure.redis.CacheNames;
import com.csports.sport.Sport;
import com.csports.sport.SportRepository;


@Service
public class SportService {
    private final SportRepository sportRepository;


    public SportService(SportRepository sportRepository) {
        this.sportRepository = sportRepository;
    }
    
    @Cacheable(cacheNames = CacheNames.SPORTS, key = "'all'", sync = true)
    public List<SportResponse> getSports() {
        List<SportResponse> sports = sportRepository.findAll()
                .stream()
                .map(sport -> new SportResponse(
                        sport.getId(),
                        sport.getName()))
                .toList();

        return sports;
    }
    @CacheEvict(cacheNames = CacheNames.SPORTS, allEntries = true)
    public Sport addSport(Sport sport) {
        Sport savedSport = sportRepository.save(sport);
        return savedSport;
    }
}
