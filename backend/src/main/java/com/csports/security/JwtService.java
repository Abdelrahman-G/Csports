package com.csports.security;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.csports.user.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private static final String TOKEN_TYPE_CLAIM = "token_type";

    private final String secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtService(
        @Value("${jwt.secret}") String secretKey,
        @Value("${jwt.access-expiration}") long accessExpiration,
        @Value("${jwt.refresh-expiration}") long refreshExpiration
    ) {
        if (secretKey == null || !secretKey.matches("^[0-9a-fA-F]{64}$")) {
            throw new IllegalArgumentException(
                    "JWT secret must contain exactly 64 hexadecimal characters.");
        }
        this.secretKey = secretKey;
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateAccessToken(User user) {
        return generateToken(user, JwtTokenType.ACCESS, accessExpiration);
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, JwtTokenType.REFRESH, refreshExpiration);
    }

    public String extractUserId(String token) {
        return parseClaims(token).getSubject();
    }

    public Duration getRemainingLifetime(String token) {
        Date expiration = parseClaims(token).getExpiration();

        long remainingMillis = expiration.getTime() - System.currentTimeMillis();
        return remainingMillis > 0 ? Duration.ofMillis(remainingMillis) : Duration.ZERO;
    }

    public LocalDateTime getExpiration(String token) {
        return LocalDateTime.ofInstant(
                parseClaims(token).getExpiration().toInstant(),
                ZoneId.systemDefault());
    }

    public boolean isAccessTokenValid(String token, User user) {
        return isTokenValid(token, user, JwtTokenType.ACCESS);
    }

    public boolean isRefreshTokenValid(String token, User user) {
        return isTokenValid(token, user, JwtTokenType.REFRESH);
    }

    private String generateToken(
            User user,
            JwtTokenType tokenType,
            long expirationMillis) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(user.getId()))
                .claim(TOKEN_TYPE_CLAIM, tokenType.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(getSigningKey())
                .compact();
    }

    private boolean isTokenValid(
            String token,
            User user,
            JwtTokenType expectedType) {
        try {
            Claims claims = parseClaims(token);
            String tokenType = claims.get(TOKEN_TYPE_CLAIM, String.class);
            return claims.getSubject().equals(String.valueOf(user.getId()))
                    && expectedType.name().equals(tokenType)
                    && claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(hexStringToByteArray(secretKey));
    }

    private byte[] hexStringToByteArray(String hex) {
        byte[] data = new byte[hex.length() / 2];
        for (int index = 0; index < hex.length(); index += 2) {
            data[index / 2] = (byte) ((Character.digit(hex.charAt(index), 16) << 4)
                    + Character.digit(hex.charAt(index + 1), 16));
        }
        return data;
    }
}
