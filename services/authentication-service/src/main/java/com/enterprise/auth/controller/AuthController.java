package com.enterprise.auth.controller;

import com.enterprise.auth.dto.*;
import com.enterprise.auth.entity.RoleType;
import com.enterprise.auth.service.AuthService;
import com.enterprise.shared.constant.AppConstants;
import com.enterprise.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login, logout, registration, token refresh and role listing")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive access + refresh tokens")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a valid refresh token for a new token pair")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Invalidate the current access and refresh tokens")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest,
                                                      @RequestBody(required = false) RefreshTokenRequest body) {
        String authHeader = httpRequest.getHeader(AppConstants.AUTHORIZATION_HEADER);
        String accessToken = (authHeader != null && authHeader.startsWith(AppConstants.BEARER_PREFIX))
                ? authHeader.substring(AppConstants.BEARER_PREFIX.length())
                : null;
        String refreshToken = body != null ? body.getRefreshToken() : null;

        authService.logout(accessToken, refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @GetMapping("/roles")
    @Operation(summary = "List all available roles in the platform")
    public ResponseEntity<ApiResponse<List<String>>> roles() {
        List<String> roles = Arrays.stream(RoleType.values()).map(Enum::name).toList();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }
}
