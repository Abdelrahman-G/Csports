package com.Csports.Csports.security;

import java.security.Key;
import java.sql.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.Csports.Csports.model.User;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
    private final String secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtService(
        @Value("${jwt.secret}") String secretKey,
        @Value("${jwt.access-expiration}") long accessExpiration,
        @Value("${jwt.refresh-expiration}") long refreshExpiration
    ) {
        this.secretKey = secretKey;
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

        private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    public String generateAccessToken(User user) {

        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(new Date(accessExpiration))
                .expiration(new Date(
                        System.currentTimeMillis() + accessExpiration))
                .signWith(getSigningKey())
                .compact();
    }
    public String generateRefreshToken(User user) {

        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(new Date(refreshExpiration))
                .expiration(new Date(
                        System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
    public boolean isTokenValid(String token, User user) {

        String username = extractUsername(token);

        return username.equals(user.getEmail()) && !isTokenExpired(token);
    }
    
    private boolean isTokenExpired(String token) {
        Date expiration = (Date) Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();

        return expiration.before(new Date(System.currentTimeMillis()));
    }
}