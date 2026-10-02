package io.github.danielmelejpinto.pedidoapi.service;

import io.github.danielmelejpinto.pedidoapi.client.InventarioClient;
import io.github.danielmelejpinto.pedidoapi.client.ProductoClient;
import io.github.danielmelejpinto.pedidoapi.client.dto.ProductoDTO;
import io.github.danielmelejpinto.pedidoapi.dto.PedidoRequest;
import io.github.danielmelejpinto.pedidoapi.model.EstadoPedido;
import io.github.danielmelejpinto.pedidoapi.model.Pedido;
import io.github.danielmelejpinto.pedidoapi.model.PedidoItem;
import io.github.danielmelejpinto.pedidoapi.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private final PedidoRepository pedidoRepository;
    private final ProductoClient productoClient;
    private final InventarioClient inventarioClient;
    private final io.github.danielmelejpinto.pedidoapi.repository.ClaveIdempotenciaRepository claveIdempotenciaRepository;

    public PedidoService(PedidoRepository pedidoRepository, ProductoClient productoClient,
            InventarioClient inventarioClient, io.github.danielmelejpinto.pedidoapi.repository.ClaveIdempotenciaRepository claveIdempotenciaRepository) {
        this.pedidoRepository = pedidoRepository;
        this.productoClient = productoClient;
        this.inventarioClient = inventarioClient;
        this.claveIdempotenciaRepository = claveIdempotenciaRepository;
    }

    @Transactional
    public Pedido crearPedido(Long usuarioId, PedidoRequest request, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            java.util.Optional<io.github.danielmelejpinto.pedidoapi.model.ClaveIdempotencia> existente = 
                claveIdempotenciaRepository.findById(idempotencyKey);
            if (existente.isPresent()) {
                log.info("Pedido idempotente recuperado para clave {}", idempotencyKey);
                return pedidoRepository.findById(existente.get().getPedidoId())
                        .orElseThrow(() -> new IllegalStateException("Pedido referenciado por idempotencia no existe"));
            }
        }

        Pedido pedido = new Pedido();
        pedido.setUsuarioId(usuarioId); 
        BigDecimal total = BigDecimal.ZERO;

        // Aquí guardaremos los productos que logramos reservar para liberarlos si algo
        // falla después
        java.util.List<PedidoItem> itemsReservados = new java.util.ArrayList<>();
        Pedido pedidoGuardado = null;

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
            
            pedido.setTotal(total);
            pedido.setEstado(EstadoPedido.CONFIRMADO);
            pedidoGuardado = pedidoRepository.saveAndFlush(pedido);

            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                claveIdempotenciaRepository.saveAndFlush(
                    new io.github.danielmelejpinto.pedidoapi.model.ClaveIdempotencia(idempotencyKey, pedidoGuardado.getId()));
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
                    log.error("Error crítico al liberar stock huérfano del producto {}", itemReservado.getProductoId(), exCompensacion);
                }
            }
            // Relanzamos la excepción para que el usuario reciba su HTTP 400/500 original
            // y para que el @Transactional aborte el guardado del Pedido en la BD local.
            throw e;
        }
        
        return pedidoGuardado;
    }

    public List<Pedido> obtenerPedidosPorUsuario(Long usuarioId) {
        return pedidoRepository.findByUsuarioId(usuarioId);
    }

    @Transactional
    public Pedido cancelarPedido(Long usuarioId, Long pedidoId) {
        Pedido pedido = pedidoRepository.findById(pedidoId)
                .orElseThrow(() -> new io.github.danielmelejpinto.pedidoapi.exception.PedidoNoEncontradoException(pedidoId));

        if (!pedido.getUsuarioId().equals(usuarioId)) {
            throw new io.github.danielmelejpinto.pedidoapi.exception.PedidoNoEncontradoException(pedidoId);
        }

        if (pedido.getEstado() != EstadoPedido.CONFIRMADO) {
            throw new io.github.danielmelejpinto.pedidoapi.exception.EstadoPedidoInvalidoException(
                    "No se puede cancelar el pedido porque está en estado " + pedido.getEstado());
        }

        // Transición atómica CONFIRMADO -> CANCELADO. Si otra cancelación ganó la carrera,
        // no se cambia ninguna fila y no se toca el stock (evita liberarlo dos veces).
        int filas = pedidoRepository.cancelarSiConfirmado(pedidoId, usuarioId);
        if (filas == 0) {
            throw new io.github.danielmelejpinto.pedidoapi.exception.EstadoPedidoInvalidoException(
                    "No se puede cancelar el pedido porque ya no está en estado CONFIRMADO");
        }

        // Liberar el stock reservado
        for (PedidoItem item : pedido.getItems()) {
            try {
                inventarioClient.liberarStock(item.getProductoId(), item.getCantidad());
            } catch (Exception e) {
                // Si la compensación falla aquí, idealmente iría a Dead Letter Queue
                log.error("Fallo al liberar stock durante cancelación. Pedido: {}, Producto: {}", 
                          pedido.getId(), item.getProductoId(), e);
                throw new io.github.danielmelejpinto.pedidoapi.exception.ServicioDependienteException(
                        "No se pudo completar la cancelación por un error en inventario-api");
            }
        }

        // El UPDATE ya dejó la fila en CANCELADO; se refleja en la entidad devuelta.
        pedido.setEstado(EstadoPedido.CANCELADO);
        return pedido;
    }
}
