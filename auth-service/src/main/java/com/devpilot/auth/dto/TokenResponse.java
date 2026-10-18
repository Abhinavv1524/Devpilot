package com.devpilot.auth.dto;

import java.time.Instant;

public record TokenResponse(String token, String tokenType, Instant expiresAt) {
}
