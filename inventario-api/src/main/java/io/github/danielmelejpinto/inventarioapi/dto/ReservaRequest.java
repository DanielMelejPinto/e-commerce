package io.github.danielmelejpinto.inventarioapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import io.swagger.v3.oas.annotations.media.Schema;

public record ReservaRequest(
    @Schema(description = "Cantidad de unidades", example = "10")
    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a cero")
    @Max(value = 100000, message = "La cantidad no puede superar 100000 unidades")
    Integer cantidad,

    @Schema(description = "ID del pedido o transacción", example = "123")
    @NotNull(message = "El ID de pedido es obligatorio")
    Long pedidoId
) {}
