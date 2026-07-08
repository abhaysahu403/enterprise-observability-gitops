package com.enterprise.shared.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * Base64-encoded secret used to sign/verify tokens. Must be identical
     * across the authentication service and every downstream service/gateway
     * that verifies tokens.
     */
    private String secret = "ZW50ZXJwcmlzZS1vYnNlcnZhYmlsaXR5LWRlbW8tc3VwZXItc2VjcmV0LWtleS1jaGFuZ2UtbWU=";

    private long accessTokenExpirationMs = 900_000;          // 15 minutes
    private long refreshTokenExpirationMs = 604_800_000;     // 7 days
    private String issuer = "enterprise-observability-demo";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public void setAccessTokenExpirationMs(long accessTokenExpirationMs) {
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    public void setRefreshTokenExpirationMs(long refreshTokenExpirationMs) {
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
