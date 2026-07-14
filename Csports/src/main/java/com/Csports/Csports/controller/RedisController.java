package com.Csports.Csports.controller;

import com.Csports.Csports.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/redis")
@RequiredArgsConstructor
public class RedisController {

    private final RedisService redisService;

    @PostMapping("/save")
    public String save() {

        redisService.save(
                "name",
                "Abdelrahman",
                Duration.ofMinutes(5)
        );

        return "Saved";
    }

    @GetMapping("/get")
    public Object get() {

        return redisService.get("name");
    }

    @DeleteMapping("/delete")
    public String delete() {

        redisService.delete("name");

        return "Deleted";
    }

}