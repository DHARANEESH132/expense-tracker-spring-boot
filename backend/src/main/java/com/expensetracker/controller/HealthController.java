package com.expensetracker.controller;

import com.expensetracker.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Health Check", description = "System liveness and readiness probe")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Check API health status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHealth() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        Map<String, Object> data = new HashMap<>();
        data.put("uptime", uptimeMs / 1000.0);
        data.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(ApiResponse.success("API is running", data));
    }
}
