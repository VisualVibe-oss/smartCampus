package com.example.demo.dto;

import com.example.demo.entity.ReservationStatus;
import java.time.Instant;

public record ReservationResponse(Long id, Long roomId, Long userId, String title,
        Instant startsAt, Instant endsAt, ReservationStatus status, Instant createdAt) {
}
