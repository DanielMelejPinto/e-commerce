package io.github.danielmelejpinto.pedidoapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record PedidoRequest(
    @NotEmpty(message = "El pedido debe tener al menos un item")
    List<@Valid PedidoItemRequest> items
) {}
