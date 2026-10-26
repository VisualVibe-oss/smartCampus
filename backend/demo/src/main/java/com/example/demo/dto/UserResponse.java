package com.example.demo.dto;

import com.example.demo.entity.Role;
import java.time.Instant;

public record UserResponse(Long id, String email, String fullName, Role role,
        boolean enabled, Instant createdAt) {
}
