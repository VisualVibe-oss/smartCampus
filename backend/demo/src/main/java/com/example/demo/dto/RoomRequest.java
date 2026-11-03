package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

public record RoomRequest(
        @NotBlank(message = "Le code est obligatoire.")
        @Size(max = 30, message = "Le code ne doit pas dépasser 30 caractères.") String code,
        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 120, message = "Le nom ne doit pas dépasser 120 caractères.") String name,
        @NotBlank(message = "Le bâtiment est obligatoire.")
        @Size(max = 120, message = "Le bâtiment ne doit pas dépasser 120 caractères.") String building,
        @NotNull(message = "La capacité est obligatoire.")
        @Positive(message = "La capacité doit être strictement positive.") Integer capacity,
        Boolean active,
        @NotNull(message = "equipmentIds est obligatoire ; utilisez [] pour aucun équipement.")
        Set<@NotNull(message = "Un identifiant d'équipement ne peut pas être null.") UUID> equipmentIds) {
}
