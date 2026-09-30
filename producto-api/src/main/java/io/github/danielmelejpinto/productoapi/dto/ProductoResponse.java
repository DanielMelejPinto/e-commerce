package io.github.danielmelejpinto.productoapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.github.danielmelejpinto.productoapi.model.EstadoProducto;

public record ProductoResponse(
        Long id,
        String nombre,
        BigDecimal precio,
        String descripcion,
        String imagenUrl,
        LocalDateTime fechaCreacion,
        EstadoProducto estado) {
}