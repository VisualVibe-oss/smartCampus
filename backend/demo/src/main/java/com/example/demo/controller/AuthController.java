package com.example.demo.controller;

import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.LoginResponse;
import com.example.demo.dto.UserResponse;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @GetMapping("/me")
    public UserResponse me(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        return service.currentUser(extractBearerToken(authorization));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        service.logout(extractBearerToken(authorization));
        return ResponseEntity.noContent().build();
    }

    private String extractBearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (!authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            throw new UnauthorizedException(
                    "Format d'authentification invalide : utilisez « Authorization: Bearer <jeton> ».");
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }
}
