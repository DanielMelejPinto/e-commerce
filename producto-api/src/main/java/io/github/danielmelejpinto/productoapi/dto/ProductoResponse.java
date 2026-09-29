package io.github.danielmelejpinto.productoapi.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoResponse(
        Long id,
        String nombre,
        BigDecimal precio,
        LocalDateTime fechaCreacion) {
}