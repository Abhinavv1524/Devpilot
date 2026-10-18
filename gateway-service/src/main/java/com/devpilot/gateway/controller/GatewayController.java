package com.devpilot.gateway.controller;

import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@SecurityRequirement(name = "bearerAuth")
public class GatewayController {
    @GetMapping("/api/gateway/status")
    public Map<String, String> getStatus() {
        return Map.of(
                "service", "gateway-service",
                "status", "running",
                "message", "Gateway service is up and running!",
                "project", "DevPilot");
    }

}
