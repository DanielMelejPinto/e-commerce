package io.github.danielmelejpinto.pedido.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record PedidoRequest(
    @NotNull(message = "El usuarioId es obligatorio")
    Long usuarioId,
    
    @NotEmpty(message = "El pedido debe tener al menos un item")
    @Valid
    List<PedidoItemRequest> items
) {}
