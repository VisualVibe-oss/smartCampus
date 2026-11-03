package com.example.demo.dto;

import java.util.Set;
import java.util.UUID;

public record RoomResponse(UUID id, String code, String name, String building, int capacity,
        boolean active, Set<UUID> equipmentIds) {
}
