package io.github.danielmelejpinto.usuarioapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Schema(example = "ana@mail.com")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        String email,

        @Schema(example = "ClaveSegura1", format = "password")
        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {
    // Igual que antes, evitamos que la contraseña se imprima en los logs
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}