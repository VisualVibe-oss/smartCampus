package com.example.demo.dto;

import com.example.demo.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank(message = "L'adresse e-mail est obligatoire.")
        @Email(message = "L'adresse e-mail doit être valide.")
        @Size(max = 255, message = "L'adresse e-mail ne doit pas dépasser 255 caractères.") String email,
        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = 8, message = "Le mot de passe doit contenir au moins huit caractères.") String password,
        @NotBlank(message = "Le nom complet est obligatoire.")
        @Size(max = 255, message = "Le nom complet ne doit pas dépasser 255 caractères.") String fullName,
        @NotNull(message = "Le rôle est obligatoire.") Role role,
        Boolean enabled) {
}
