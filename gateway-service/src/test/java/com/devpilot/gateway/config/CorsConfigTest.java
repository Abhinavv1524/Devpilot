package com.devpilot.gateway.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.reactive.CorsWebFilter;

import reactor.core.publisher.Mono;

class CorsConfigTest {

    private final CorsWebFilter corsWebFilter =
            new CorsConfig().corsWebFilter(new String[] { "http://localhost:5173" });

    @Test
    void allowsFrontendPreflightForProxiedOpenApiRoutes() {
        for (String path : new String[] {
                "http://localhost:8080/api/tasks",
                "http://localhost:8080/orchestrator/v3/api-docs"
        }) {
            MockServerWebExchange exchange = MockServerWebExchange.from(
                    MockServerHttpRequest.options(path)
                            .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"));

            corsWebFilter.filter(exchange, ignored -> Mono.empty()).block();

            assertEquals(
                    "http://localhost:5173",
                    exchange.getResponse().getHeaders()
                            .getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
            assertEquals(
                    "authorization",
                    exchange.getResponse().getHeaders()
                            .getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS));
        }
    }

    @Test
    void doesNotAllowUnconfiguredFrontendOrigins() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.options("http://localhost:8080/api/tasks")
                        .header(HttpHeaders.ORIGIN, "https://untrusted.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"));

        corsWebFilter.filter(exchange, ignored -> Mono.empty()).block();

        assertNull(exchange.getResponse().getHeaders()
                .getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
