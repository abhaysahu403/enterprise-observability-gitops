package com.enterprise.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Entry point for the Authentication Service.
 * Component scan is widened to com.enterprise so the shared-library's
 * GlobalExceptionHandler, RequestLoggingFilter, and JwtUtil are picked up.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.enterprise.auth", "com.enterprise.shared"})
public class AuthenticationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthenticationServiceApplication.class, args);
    }
}
