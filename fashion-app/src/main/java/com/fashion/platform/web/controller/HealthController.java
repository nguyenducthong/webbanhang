package com.fashion.platform.web.controller;

import com.fashion.platform.common.security.SecurityContextHelper;
import com.fashion.platform.common.security.UserContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/system")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "fashion-platform",
                "timestamp", Instant.now().toString(),
                "sprint", "Sprint 1 - Foundation"
        ));
    }

    @GetMapping("/context")
    public ResponseEntity<UserContext> getCurrentUserContext() {
        return ResponseEntity.ok(SecurityContextHelper.getContext().orElse(null));
    }
}

