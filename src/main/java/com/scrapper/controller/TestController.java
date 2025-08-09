package com.scrapper.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
            "status", "OK",
            "timestamp", Instant.now().toString()
        );
    }

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        return Map.of("pong", true, "ts", Instant.now().toEpochMilli());
    }
}
