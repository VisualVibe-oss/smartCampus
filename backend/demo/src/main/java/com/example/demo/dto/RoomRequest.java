package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record RoomRequest(
        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 255, message = "Le nom ne doit pas dépasser 255 caractères.") String name,
        @NotBlank(message = "Le bâtiment est obligatoire.")
        @Size(max = 255, message = "Le bâtiment ne doit pas dépasser 255 caractères.") String building,
        @NotNull(message = "La capacité est obligatoire.")
        @Positive(message = "La capacité doit être strictement positive.") Integer capacity,
        String description,
        Boolean active,
        @NotNull(message = "equipmentIds est obligatoire ; utilisez [] pour aucun équipement.")
        Set<@NotNull(message = "Un identifiant d'équipement ne peut pas être null.")
                @Positive(message = "Un identifiant d'équipement doit être positif.") Long> equipmentIds) {
}
