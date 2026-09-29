package io.github.danielmelejpinto.pedido.service;

import io.github.danielmelejpinto.pedido.client.InventarioClient;
import io.github.danielmelejpinto.pedido.client.ProductoClient;
import io.github.danielmelejpinto.pedido.client.dto.ProductoDTO;
import io.github.danielmelejpinto.pedido.dto.PedidoRequest;
import io.github.danielmelejpinto.pedido.model.EstadoPedido;
import io.github.danielmelejpinto.pedido.model.Pedido;
import io.github.danielmelejpinto.pedido.model.PedidoItem;
import io.github.danielmelejpinto.pedido.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ProductoClient productoClient;
    private final InventarioClient inventarioClient;

    public PedidoService(PedidoRepository pedidoRepository, ProductoClient productoClient, InventarioClient inventarioClient) {
        this.pedidoRepository = pedidoRepository;
        this.productoClient = productoClient;
        this.inventarioClient = inventarioClient;
    }

    @Transactional
    public Pedido crearPedido(PedidoRequest request) {
        Pedido pedido = new Pedido();
        pedido.setUsuarioId(request.usuarioId());
        
        BigDecimal total = BigDecimal.ZERO;
        
        for (var itemReq : request.items()) {
            // 1. Obtener producto y validar que exista
            ProductoDTO producto = productoClient.obtenerProducto(itemReq.productoId());
            
            // 2. Reservar stock en el inventario
            inventarioClient.reservarStock(itemReq.productoId(), itemReq.cantidad());
            
            // 3. Crear el item del pedido
            PedidoItem item = new PedidoItem();
            item.setProductoId(itemReq.productoId());
            item.setCantidad(itemReq.cantidad());
            item.setPrecioUnitario(producto.precio());
            
            total = total.add(producto.precio().multiply(new BigDecimal(itemReq.cantidad())));
            pedido.addItem(item);
        }
        
        pedido.setTotal(total);
        pedido.setEstado(EstadoPedido.CONFIRMADO); // Si todo sale bien, lo confirmamos
        return pedidoRepository.save(pedido);
    }

    public List<Pedido> obtenerPedidosPorUsuario(Long usuarioId) {
        return pedidoRepository.findByUsuarioId(usuarioId);
    }
}
