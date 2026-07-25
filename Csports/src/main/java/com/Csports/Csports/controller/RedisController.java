package com.Csports.Csports.controller;

import com.Csports.Csports.DTO.SportResponse;
import com.Csports.Csports.service.RedisService;
import com.Csports.Csports.service.TokenBlacklistService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/redis")
public class RedisController {

    private final RedisService redisService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisConnectionFactory connectionFactory;
    private final TokenBlacklistService tokenBlacklistService;

    public RedisController(RedisService redisService,
                           RedisTemplate<String, Object> redisTemplate,
                           RedisConnectionFactory connectionFactory,
                           TokenBlacklistService tokenBlacklistService) {
        this.redisService = redisService;
        this.redisTemplate = redisTemplate;
        this.connectionFactory = connectionFactory;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @PostMapping("/save")
    public String save() {
        redisService.save("name", "Abdelrahman", Duration.ofMinutes(5));
        return "Saved";
    }

    @GetMapping("/get")
    public Object get() {
        return redisService.get("name");
    }

    @GetMapping("/exists")
    public boolean exists() {
        return redisService.exists("name");
    }

    @DeleteMapping("/delete")
    public String delete() {
        redisService.delete("name");
        return "Deleted";
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        try {
            connectionFactory.getConnection().ping();
            return Map.of("status", "UP", "message", "Redis is reachable");
        } catch (Exception e) {
            return Map.of("status", "DOWN", "message", e.getMessage());
        }
    }

    @GetMapping("/keys")
    public Collection<String> keys() {
        return redisTemplate.keys("*");
    }

    @GetMapping("/cache/{cacheName}")
    public Map<String, Object> inspectCache(@PathVariable String cacheName) {
        String pattern = "spring:cache:" + cacheName + "::*";
        return Map.of(
                "cache", cacheName,
                "keys", redisTemplate.keys(pattern)
        );
    }

    @PostMapping("/cache/save-object")
    public String saveObject() {
        SportResponse sport = new SportResponse(1L, "Football");
        redisService.save("sport:1", sport, Duration.ofHours(1));
        return "Sport object saved to Redis";
    }

    @GetMapping("/cache/get-object")
    public Object getObject() {
        return redisService.get("sport:1");
    }

    @PostMapping("/blacklist")
    public String blacklist(@RequestParam String token, @RequestParam(defaultValue = "15") long minutes) {
        tokenBlacklistService.blacklist(token, Duration.ofMinutes(minutes));
        return "Token blacklisted for " + minutes + " minutes";
    }

    @GetMapping("/blacklist/check")
    public Map<String, Object> checkBlacklist(@RequestParam String token) {
        return Map.of(
                "token", token,
                "blacklisted", tokenBlacklistService.isBlacklisted(token)
        );
    }
}
