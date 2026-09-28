package io.github.danielmelejpinto.productoapi.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class ProductoRequest {
    
    // @NotBlank verifica que no venga nulo ni vacío (solo para textos)
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    // @NotNull para números, y @Positive para que no pongan precios negativos o cero
    @NotNull(message = "El precio es obligatorio")
    @Positive(message = "El precio debe ser mayor a cero")
    private BigDecimal precio;

    // Getters y Setters
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }
}