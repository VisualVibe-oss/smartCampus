package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EquipmentRequest(
        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 255, message = "Le nom ne doit pas dépasser 255 caractères.") String name) {
}
