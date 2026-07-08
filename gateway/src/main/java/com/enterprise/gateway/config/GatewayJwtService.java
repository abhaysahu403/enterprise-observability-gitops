package com.enterprise.gateway.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

/**
 * Standalone JWT validator for the reactive gateway. The gateway does not
 * depend on the shared-library's servlet-based JwtUtil because Spring Cloud
 * Gateway runs on WebFlux/Netty, not the Servlet stack. Signing algorithm
 * and secret must stay in sync with com.enterprise.shared.security.JwtUtil.
 */
@Component
public class GatewayJwtService {

    private final SecretKey signingKey;

    public GatewayJwtService(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    public Optional<Claims> validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(Claims claims) {
        return (List<String>) (List<?>) claims.get("roles", List.class);
    }
}
