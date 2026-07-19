package com.Csports.Csports.service;

import java.time.Duration;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;

import com.Csports.Csports.model.User;
import com.Csports.Csports.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RedisService redisService;
    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(30);
    private static final String USER_CACHE_PREFIX = "user:";
    
    public CustomUserDetailsService(UserRepository userRepository, RedisService redisService) {
        this.userRepository = userRepository;
        this.redisService = redisService;
    }

    @Override
    public UserDetails loadUserByUsername(String userId) {

        String cacheKey = USER_CACHE_PREFIX + userId;
        User cachedUser = redisService.get(cacheKey,new TypeReference<User>() {});
        
        if (cachedUser == null) {
            cachedUser = userRepository.findById(Long.parseLong(userId)).orElseThrow(() -> new UsernameNotFoundException("User not found"));
            redisService.save(cacheKey, cachedUser, USER_CACHE_TTL);
        }

        return cachedUser;
    }
}