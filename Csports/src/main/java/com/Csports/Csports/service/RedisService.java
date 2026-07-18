package com.Csports.Csports.service;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;


import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> void save(String key, T value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public <T> T get(String key, TypeReference<T> type) {

        Object value = redisTemplate.opsForValue().get(key);

        if (value == null)
            return null;

        return objectMapper.convertValue(value, type);
    }

    public <T> void save(String key, T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    // public <T> void save(String key, T value, Duration ttl) {
    //     redisTemplate.opsForValue().set(key, value, ttl);
    // }

    // @SuppressWarnings("unchecked")
    // public <T> T get(String key) {
    //     return (T) redisTemplate.opsForValue().get(key);
    // }

    public <T> T get(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        return clazz.cast(value);
    }

    public boolean exists(String key) {
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}