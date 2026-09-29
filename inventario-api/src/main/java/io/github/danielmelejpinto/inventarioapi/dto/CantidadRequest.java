package io.github.danielmelejpinto.inventarioapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CantidadRequest(
    @Schema(description = "Cantidad de unidades (entre 1 y 100000)", example = "10")
    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser mayor a cero")
    @Max(value = 100000, message = "La cantidad no puede superar 100000 unidades por operación")
    Integer cantidad
) {}