package io.github.danielmelejpinto.pedidoapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PedidoRequest(
    @NotEmpty(message = "El pedido debe tener al menos un item")
    @Size(max = 100, message = "El pedido no puede tener más de 100 items")
    List<@NotNull @Valid PedidoItemRequest> items
) {}
