package com.enterprise.auth.service;

import com.enterprise.auth.dto.*;
import com.enterprise.auth.entity.RoleType;
import com.enterprise.auth.entity.User;
import com.enterprise.auth.repository.UserRepository;
import com.enterprise.shared.constant.AppConstants;
import com.enterprise.shared.exception.BusinessException;
import com.enterprise.shared.exception.UnauthorizedException;
import com.enterprise.shared.security.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;

    public AuthService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        JwtUtil jwtUtil,
                        StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("DUPLICATE_USERNAME", "Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("DUPLICATE_EMAIL", "Email '" + request.getEmail() + "' is already registered");
        }

        Set<RoleType> roles = request.getRoles().stream()
                .map(r -> {
                    try {
                        return RoleType.valueOf(r.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new BusinessException("INVALID_ROLE", "Unknown role: " + r);
                    }
                })
                .collect(Collectors.toSet());

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRoles(roles);
        user.setEnabled(true);

        User saved = userRepository.save(user);
        log.info("New user registered: username={} roles={}", saved.getUsername(), roles);
        return UserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException e) {
            log.warn("Failed login attempt for username={}", request.getUsername());
            throw e;
        }

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid username or password"));

        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account is disabled. Contact an administrator.");
        }

        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(RefreshTokenRequest request) {
        String token = request.getRefreshToken();

        if (isBlacklisted(token)) {
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        try {
            if (!jwtUtil.isTokenValid(token) || !jwtUtil.isRefreshToken(token)) {
                throw new UnauthorizedException("Invalid refresh token");
            }
        } catch (JwtException e) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        String username = jwtUtil.extractUsername(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));

        if (!user.isEnabled()) {
            throw new UnauthorizedException("Account is disabled");
        }

        // rotate: blacklist the old refresh token before issuing a new pair
        blacklist(token);
        return issueTokens(user);
    }

    public void logout(String accessToken, String refreshToken) {
        if (accessToken != null) {
            blacklist(accessToken);
        }
        if (refreshToken != null) {
            blacklist(refreshToken);
        }
        log.info("User logged out, tokens blacklisted");
    }

    private TokenResponse issueTokens(User user) {
        List<String> roles = user.getRoles().stream().map(Enum::name).collect(Collectors.toList());
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), roles);
        return new TokenResponse(accessToken, refreshToken, 900, UserResponse.from(user));
    }

    private void blacklist(String token) {
        try {
            var claims = jwtUtil.parseClaims(token);
            long ttlMs = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (ttlMs > 0) {
                redisTemplate.opsForValue().set(
                        AppConstants.CACHE_TOKEN_BLACKLIST + ":" + token, "1", Duration.ofMillis(ttlMs));
            }
        } catch (JwtException ignored) {
            // token already invalid/expired - nothing to blacklist
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(
                redisTemplate.hasKey(AppConstants.CACHE_TOKEN_BLACKLIST + ":" + token));
    }
}
