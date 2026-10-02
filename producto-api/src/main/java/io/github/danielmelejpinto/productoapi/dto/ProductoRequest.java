package io.github.danielmelejpinto.productoapi.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProductoRequest(
        @Schema(description = "Nombre del producto", example = "Teclado mecánico")
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @Schema(description = "Precio (hasta 17 enteros y 2 decimales)", example = "49.90")
        @NotNull(message = "El precio es obligatorio")
        @Positive(message = "El precio debe ser mayor a cero")
        @Digits(integer = 17, fraction = 2, message = "El precio admite hasta 17 enteros y 2 decimales")
        BigDecimal precio,
        
        @Schema(description = "Descripción detallada del producto", example = "Teclado mecánico RGB")
        @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres")
        String descripcion,

        @Schema(description = "URL de la imagen del producto", example = "https://ejemplo.com/imagen.jpg")
        @Size(max = 2000, message = "La URL de la imagen no puede superar 2000 caracteres")
        String imagenUrl) {
}
