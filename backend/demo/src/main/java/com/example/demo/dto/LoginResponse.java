package com.example.demo.dto;

import java.time.Instant;

public record LoginResponse(String token, String tokenType, Instant expiresAt, UserResponse user) {
}
