package com.Csports.Csports.security;

import java.security.Key;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.Csports.Csports.model.User;

import io.jsonwebtoken.Jwts;
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
                byte[] keyBytes = hexStringToByteArray(secretKey);
                return Keys.hmacShaKeyFor(keyBytes);
        }

        private byte[] hexStringToByteArray(String hex) {
                int len = hex.length();
                byte[] data = new byte[len / 2];
                for (int i = 0; i < len; i += 2) {
                        data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                                        + Character.digit(hex.charAt(i + 1), 16));
                }
                return data;
        }
    public String generateAccessToken(User user) {

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(
                        System.currentTimeMillis() + accessExpiration))
                .signWith(getSigningKey())
                .compact();
    }
    public String generateRefreshToken(User user) {

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(
                        System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUserId(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }
    public boolean isTokenValid(String token, User userDetails) {

        String userId = extractUserId(token);

        return userId.equals(String.valueOf(userDetails.getId())) && !isTokenExpired(token);
    }
    
    private boolean isTokenExpired(String token) {
        Date expiration = Jwts.parser()
                .verifyWith((SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();

        return expiration.before(new Date());
    }
}