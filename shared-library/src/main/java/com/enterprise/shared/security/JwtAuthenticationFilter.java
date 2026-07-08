package com.enterprise.shared.security;

import com.enterprise.shared.constant.AppConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validates the Bearer token on every incoming request and, if valid,
 * populates the Spring Security context with the username and roles
 * extracted from the JWT claims. Stateless - no session is created.
 *
 * Every service (except the Authentication Service's public endpoints and
 * the Gateway, which forwards identity headers instead) registers this
 * filter in its SecurityFilterChain.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(AppConstants.AUTHORIZATION_HEADER);

        if (header != null && header.startsWith(AppConstants.BEARER_PREFIX)) {
            String token = header.substring(AppConstants.BEARER_PREFIX.length());

            if (jwtUtil.isTokenValid(token) && !jwtUtil.isRefreshToken(token)) {
                String username = jwtUtil.extractUsername(token);
                List<String> roles = jwtUtil.extractRoles(token);

                List<GrantedAuthority> authorities = roles.stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                MDC.put(AppConstants.MDC_USER, username);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(AppConstants.MDC_USER);
        }
    }
}
