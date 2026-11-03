package com.example.demo.dto;

import com.example.demo.entity.ReservationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public record ReservationRequest(
        @NotNull(message = "roomId est obligatoire.") UUID roomId,
        @NotNull(message = "userId est obligatoire.") UUID userId,
        @NotNull(message = "startAt est obligatoire.") Instant startAt,
        ReservationStatus status,
        UUID revokedBy,
        @Size(max = 1000, message = "Le motif ne doit pas dépasser 1000 caractères.") String revocationReason) {
}
