package com.devpilot.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutesConfig {

    @Bean
    RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", route -> route
                        .path("/api/auth/**")
                        .uri("lb://auth-service"))
                .route("orchestrator-service", route -> route
                        .path("/api/tasks/**")
                        .uri("lb://orchestrator-service"))
                .route("task-history", route -> route
                        .path("/tasks/history", "/tasks/history/**")
                        .uri("lb://orchestrator-service"))
                .route("ai-agent-service", route -> route
                        .path("/api/agents/**", "/api/knowledge/**")
                        .uri("lb://ai-agent-service"))
                .route("orchestrator-openapi", route -> route
                        .path("/orchestrator/v3/api-docs", "/orchestrator/v3/api-docs/**")
                        .filters(filters -> filters.rewritePath(
                                "/orchestrator/(?<segment>.*)", "/${segment}"))
                        .uri("lb://orchestrator-service"))
                .route("ai-agent-openapi", route -> route
                        .path("/ai-agent/v3/api-docs", "/ai-agent/v3/api-docs/**")
                        .filters(filters -> filters.rewritePath(
                                "/ai-agent/(?<segment>.*)", "/${segment}"))
                        .uri("lb://ai-agent-service"))
                .route("auth-openapi", route -> route
                        .path("/auth/v3/api-docs", "/auth/v3/api-docs/**")
                        .filters(filters -> filters.rewritePath(
                                "/auth/(?<segment>.*)", "/${segment}"))
                        .uri("lb://auth-service"))
                .build();
    }
}
