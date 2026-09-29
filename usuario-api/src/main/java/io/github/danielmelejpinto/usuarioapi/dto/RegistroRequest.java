package io.github.danielmelejpinto.usuarioapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @Schema(description = "Nombre completo", example = "Ana Pérez")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @Schema(description = "Email (se guarda en minúsculas)", example = "ana@mail.com")
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        @Size(max = 254, message = "El email no puede superar 254 caracteres")
        String email,

        @Schema(description = "Contraseña (8 a 72 caracteres, máximo 72 bytes)", example = "ClaveSegura1", format = "password")
        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 72, message = "La contraseña debe tener entre 8 y 72 caracteres")
        String password) {

    // El toString automático de un record imprimiría la contraseña: si alguien
    // loguea el request por accidente, no debe salir
    @Override
    public String toString() {
        return "RegistroRequest[email=" + email + "]";
    }
}