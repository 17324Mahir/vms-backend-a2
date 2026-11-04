package com.starkgroup.vms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public root controller providing service discovery and status metadata.
 */
@RestController
public class RootController {

    @GetMapping(value = {"/", "/api"})
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", "Stark Group Vehicle Management System API");
        info.put("status", "UP");
        info.put("version", "1.0.0");

        Map<String, String> endpoints = new LinkedHashMap<>();
        endpoints.put("health", "/actuator/health");
        endpoints.put("login", "POST /api/auth/login");
        endpoints.put("register", "POST /api/auth/register");
        endpoints.put("vehicles", "/api/vehicles");
        info.put("endpoints", endpoints);

        return ResponseEntity.ok(info);
    }
}

