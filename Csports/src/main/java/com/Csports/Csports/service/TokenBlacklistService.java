package com.Csports.Csports.service;

import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.Csports.Csports.config.RedisKeys;
import com.Csports.Csports.security.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtService jwtService;

    public void blacklist(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Access token is required");
        }

        Duration remainingLifetime = jwtService.getRemainingLifetime(token);
        if (remainingLifetime.isPositive()) {
            redisTemplate.opsForValue().set(keyFor(token), true, remainingLifetime);
        }
    }

    public boolean isBlacklisted(String token) {
        return token != null && Boolean.TRUE.equals(redisTemplate.hasKey(keyFor(token)));
    }

    public void evict(String token) {
        if (token != null) {
            redisTemplate.delete(keyFor(token));
        }
    }

    private String keyFor(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return RedisKeys.TOKEN_BLACKLIST_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
