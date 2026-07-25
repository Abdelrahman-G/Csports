package com.Csports.Csports.service;

import java.time.Duration;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:token:";

    public void blacklist(String token, Duration ttl) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, true, ttl);
    }

    public boolean isBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void evict(String token) {
        redisTemplate.delete(BLACKLIST_PREFIX + token);
    }
}
