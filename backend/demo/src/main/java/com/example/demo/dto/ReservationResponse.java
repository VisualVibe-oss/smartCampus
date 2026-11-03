package com.example.demo.dto;

import com.example.demo.entity.ReservationStatus;
import java.time.Instant;
import java.util.UUID;

public record ReservationResponse(UUID id, UUID roomId, UUID userId, Instant startAt, Instant endAt,
        ReservationStatus status, Instant createdAt, Instant cancelledAt, UUID revokedBy,
        Instant revokedAt, String revocationReason) {
}
