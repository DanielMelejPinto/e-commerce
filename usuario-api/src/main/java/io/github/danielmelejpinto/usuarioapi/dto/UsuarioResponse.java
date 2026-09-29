package io.github.danielmelejpinto.usuarioapi.dto;

import java.time.LocalDateTime;

import io.github.danielmelejpinto.usuarioapi.model.EstadoUsuario;
import io.github.danielmelejpinto.usuarioapi.model.Rol;

// Nunca incluye la contraseña ni su hash
public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        Rol rol,
        EstadoUsuario estado,
        LocalDateTime fechaCreacion) {
}