package io.github.danielmelejpinto.pedidoapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PedidoItemRequest(
    @NotNull(message = "El productoId es obligatorio")
    Long productoId,
    
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a cero")
    Integer cantidad
) {}
