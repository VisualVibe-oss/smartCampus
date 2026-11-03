package com.example.demo.dto;

import com.example.demo.entity.Role;
import java.util.UUID;

public record UserResponse(UUID id, String email, String fullName, Role role,
        boolean active, java.time.Instant createdAt) {
}
