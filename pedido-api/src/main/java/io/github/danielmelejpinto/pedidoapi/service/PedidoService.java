package io.github.danielmelejpinto.pedidoapi.service;

import io.github.danielmelejpinto.pedidoapi.client.InventarioClient;
import io.github.danielmelejpinto.pedidoapi.client.ProductoClient;
import io.github.danielmelejpinto.pedidoapi.client.dto.ProductoDTO;
import io.github.danielmelejpinto.pedidoapi.dto.PedidoRequest;
import io.github.danielmelejpinto.pedidoapi.model.EstadoPedido;
import io.github.danielmelejpinto.pedidoapi.model.Pedido;
import io.github.danielmelejpinto.pedidoapi.model.PedidoItem;
import io.github.danielmelejpinto.pedidoapi.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoClient productoClient;
    private final InventarioClient inventarioClient;

    public PedidoService(PedidoRepository pedidoRepository, ProductoClient productoClient,
            InventarioClient inventarioClient) {
        this.pedidoRepository = pedidoRepository;
        this.productoClient = productoClient;
        this.inventarioClient = inventarioClient;
    }

    @Transactional
    public Pedido crearPedido(Long usuarioId, PedidoRequest request) {
        Pedido pedido = new Pedido();
        pedido.setUsuarioId(usuarioId); 
        BigDecimal total = BigDecimal.ZERO;

        // Aquí guardaremos los productos que logramos reservar para liberarlos si algo
        // falla después
        java.util.List<PedidoItem> itemsReservados = new java.util.ArrayList<>();

        try {
            for (var itemReq : request.items()) {
                ProductoDTO producto = productoClient.obtenerProducto(itemReq.productoId());

                inventarioClient.reservarStock(itemReq.productoId(), itemReq.cantidad());

                PedidoItem item = new PedidoItem();
                item.setProductoId(itemReq.productoId());
                item.setCantidad(itemReq.cantidad());
                item.setPrecioUnitario(producto.precio());

                total = total.add(producto.precio().multiply(new BigDecimal(itemReq.cantidad())));
                pedido.addItem(item);

                // Anotamos que este item ya fue reservado exitosamente en el servicio externo
                itemsReservados.add(item);
            }
        } catch (Exception e) {
            // ¡Algo falló! Activamos la transacción compensatoria (Saga)
            for (PedidoItem itemReservado : itemsReservados) {
                try {
                    inventarioClient.liberarStock(itemReservado.getProductoId(), itemReservado.getCantidad());
                } catch (Exception exCompensacion) {
                    // En sistemas avanzados, si la compensación falla, se manda a una "Dead Letter
                    // Queue" (Kafka/RabbitMQ)
                    // para revisión manual. Por ahora, solo lo logueamos.
                    System.err.println(
                            "Error crítico al liberar stock huérfano del producto " + itemReservado.getProductoId());
                }
            }
            // Relanzamos la excepción para que el usuario reciba su HTTP 400/500 original
            // y para que el @Transactional aborte el guardado del Pedido en la BD local.
            throw e;
        }

        pedido.setTotal(total);
        pedido.setEstado(EstadoPedido.CONFIRMADO);
        return pedidoRepository.save(pedido);
    }

    public List<Pedido> obtenerPedidosPorUsuario(Long usuarioId) {
        return pedidoRepository.findByUsuarioId(usuarioId);
    }
}
