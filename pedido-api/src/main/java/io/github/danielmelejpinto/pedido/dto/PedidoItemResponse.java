package io.github.danielmelejpinto.pedido.dto;

import io.github.danielmelejpinto.pedido.model.PedidoItem;
import java.math.BigDecimal;

public record PedidoItemResponse(
    Long id,
    Long productoId,
    Integer cantidad,
    BigDecimal precioUnitario,
    BigDecimal subtotal
) {
    // Método de utilidad para convertir la Entidad al DTO
    public static PedidoItemResponse fromEntity(PedidoItem item) {
        return new PedidoItemResponse(
            item.getId(),
            item.getProductoId(),
            item.getCantidad(),
            item.getPrecioUnitario(),
            item.getPrecioUnitario().multiply(new BigDecimal(item.getCantidad()))
        );
    }
}