package com.devpilot.auth.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.security.Keys;

import com.devpilot.auth.dto.TokenResponse;
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMinutes;

    public JwtService(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-minutes:60}") long expirationMinutes) {
        this.signingKey = createSigningKey(secret);
        this.expirationMinutes = expirationMinutes;
    }

    public TokenResponse createToken(String username) {
        Instant expiresAt = Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES);
        String token = Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
        return new TokenResponse(token, "Bearer", expiresAt);
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    private SecretKey createSigningKey(String secret) {
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        } catch (DecodingException | IllegalArgumentException exception) {
            return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }
}
