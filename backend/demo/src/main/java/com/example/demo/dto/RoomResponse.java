package com.example.demo.dto;

import java.util.Set;

public record RoomResponse(Long id, String name, String building, int capacity,
        String description, boolean active, Set<Long> equipmentIds) {
}
