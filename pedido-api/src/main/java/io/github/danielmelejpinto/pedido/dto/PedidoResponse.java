package io.github.danielmelejpinto.pedido.dto;

import io.github.danielmelejpinto.pedido.model.Pedido;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record PedidoResponse(
    Long id,
    Long usuarioId,
    String estado,
    BigDecimal total,
    LocalDateTime fechaCreacion,
    List<PedidoItemResponse> items
) {
    // Método de utilidad para mapear el Pedido completo y su lista
    public static PedidoResponse fromEntity(Pedido pedido) {
        List<PedidoItemResponse> itemsDto = pedido.getItems().stream()
            .map(PedidoItemResponse::fromEntity)
            .collect(Collectors.toList());

        return new PedidoResponse(
            pedido.getId(),
            pedido.getUsuarioId(),
            pedido.getEstado().name(),
            pedido.getTotal(),
            pedido.getFechaCreacion(),
            itemsDto
        );
    }
}