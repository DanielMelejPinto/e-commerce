package io.github.danielmelejpinto.inventarioapi.dto;

import java.time.LocalDateTime;

public record InventarioResponse(
    Long productoId,
    Long cantidadDisponible,
    Long cantidadReservada,
    LocalDateTime ultimaActualizacion
) {}