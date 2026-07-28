package com.csports.infrastructure.redis;

import java.time.Duration;
import java.util.Optional;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> void save(String key, T value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public <T> void save(String key, T value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null
                ? Optional.empty()
                : Optional.of(objectMapper.convertValue(value, type));
    }

    public <T> Optional<T> get(String key, TypeReference<T> type) {
        Object value = redisTemplate.opsForValue().get(key);
        return value == null
                ? Optional.empty()
                : Optional.of(objectMapper.convertValue(value, type));
    }

    public boolean exists(String key) {
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }
}
