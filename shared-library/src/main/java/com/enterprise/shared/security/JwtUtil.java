package com.enterprise.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shared JWT utility used by the Authentication Service to mint tokens and
 * by every other service / the gateway to validate them. Centralizing this
 * logic guarantees all services agree on claim names and signing algorithm.
 */
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class JwtUtil {

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_TOKEN_TYPE = "type";

    private final JwtProperties properties;
    private final SecretKey signingKey;

    @Autowired
    public JwtUtil(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(properties.getSecret()));
    }

    public String generateAccessToken(Long userId, String username, List<String> roles) {
        return buildToken(userId, username, roles, "ACCESS", properties.getAccessTokenExpirationMs());
    }

    public String generateRefreshToken(Long userId, String username, List<String> roles) {
        return buildToken(userId, username, roles, "REFRESH", properties.getRefreshTokenExpirationMs());
    }

    private String buildToken(Long userId, String username, List<String> roles, String type, long expiryMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiryMs);

        return Jwts.builder()
                .subject(username)
                .issuer(properties.getIssuer())
                .claim(CLAIM_USER_ID, userId)
                .claim(CLAIM_ROLES, roles)
                .claim(CLAIM_TOKEN_TYPE, type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            return false;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractUserId(String token) {
        Claims claims = parseClaims(token);
        Object userId = claims.get(CLAIM_USER_ID);
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return (Long) userId;
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = parseClaims(token);
        List<?> raw = claims.get(CLAIM_ROLES, List.class);
        return raw.stream().map(Object::toString).collect(Collectors.toList());
    }

    public String extractTokenType(String token) {
        return parseClaims(token).get(CLAIM_TOKEN_TYPE, String.class);
    }

    public boolean isRefreshToken(String token) {
        return "REFRESH".equals(extractTokenType(token));
    }

    public Map<String, Object> extractAllClaims(String token) {
        return parseClaims(token);
    }
}
