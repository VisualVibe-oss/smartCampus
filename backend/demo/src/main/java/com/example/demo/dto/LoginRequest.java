package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "L'adresse e-mail est obligatoire.")
        @Email(message = "L'adresse e-mail doit être valide.")
        @Size(max = 255, message = "L'adresse e-mail ne doit pas dépasser 255 caractères.") String email,
        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(max = 72, message = "Le mot de passe ne doit pas dépasser 72 caractères.")
        String password) {

    public LoginRequest {
        // La validation @Email s'applique apres cette normalisation.
        email = email == null ? null : email.trim();
    }
}
