package com.example.demo.dto;

import com.example.demo.entity.ReservationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record ReservationRequest(
        @NotNull(message = "roomId est obligatoire.")
        @Positive(message = "roomId doit être positif.") Long roomId,
        @NotNull(message = "userId est obligatoire.")
        @Positive(message = "userId doit être positif.") Long userId,
        @NotBlank(message = "Le titre est obligatoire.")
        @Size(max = 255, message = "Le titre ne doit pas dépasser 255 caractères.") String title,
        @NotNull(message = "startsAt est obligatoire.") Instant startsAt,
        @NotNull(message = "endsAt est obligatoire.") Instant endsAt,
        ReservationStatus status) {
}
