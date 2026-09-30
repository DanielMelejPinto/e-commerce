package io.github.danielmelejpinto.pedidoapi.service;

import io.github.danielmelejpinto.pedidoapi.client.InventarioClient;
import io.github.danielmelejpinto.pedidoapi.client.ProductoClient;
import io.github.danielmelejpinto.pedidoapi.client.dto.ProductoDTO;
import io.github.danielmelejpinto.pedidoapi.dto.PedidoItemRequest;
import io.github.danielmelejpinto.pedidoapi.dto.PedidoRequest;
import io.github.danielmelejpinto.pedidoapi.model.EstadoPedido;
import io.github.danielmelejpinto.pedidoapi.model.Pedido;
import io.github.danielmelejpinto.pedidoapi.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProductoClient productoClient;

    @Mock
    private InventarioClient inventarioClient;

    @InjectMocks
    private PedidoService pedidoService;

    @Test
    void crearPedido_exitoso() {
        // Arrange
        Long usuarioId = 1L;
        Long productoId = 100L;
        Integer cantidad = 2;
        BigDecimal precio = new BigDecimal("50.00");

        PedidoItemRequest itemRequest = new PedidoItemRequest(productoId, cantidad);
        // Sin usuarioId en el request
        PedidoRequest request = new PedidoRequest(List.of(itemRequest));

        ProductoDTO productoDTO = new ProductoDTO(productoId, "Producto Test", precio, "ACTIVO");

        when(productoClient.obtenerProducto(productoId)).thenReturn(productoDTO);
        doNothing().when(inventarioClient).reservarStock(productoId, cantidad);
        
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        // Act (Se le pasa el usuarioId como primer parámetro)
        Pedido pedidoCreado = pedidoService.crearPedido(usuarioId, request);

        // Assert
        assertNotNull(pedidoCreado);
        assertEquals(usuarioId, pedidoCreado.getUsuarioId());
        assertEquals(EstadoPedido.CONFIRMADO, pedidoCreado.getEstado());
        assertEquals(new BigDecimal("100.00"), pedidoCreado.getTotal());
        assertEquals(1, pedidoCreado.getItems().size());
        
        verify(productoClient, times(1)).obtenerProducto(productoId);
        verify(inventarioClient, times(1)).reservarStock(productoId, cantidad);
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }

    @Test
    void cancelarPedido_exitoso() {
        // Arrange
        Long pedidoId = 1L;
        Long usuarioId = 1L;
        Pedido pedido = new Pedido();
        pedido.setId(pedidoId);
        pedido.setUsuarioId(usuarioId);
        pedido.setEstado(EstadoPedido.CONFIRMADO);
        io.github.danielmelejpinto.pedidoapi.model.PedidoItem item = new io.github.danielmelejpinto.pedidoapi.model.PedidoItem();
        item.setProductoId(10L);
        item.setCantidad(2);
        pedido.addItem(item);

        when(pedidoRepository.findById(pedidoId)).thenReturn(java.util.Optional.of(pedido));
        doNothing().when(inventarioClient).liberarStock(10L, 2);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Pedido cancelado = pedidoService.cancelarPedido(usuarioId, pedidoId);

        // Assert
        assertEquals(EstadoPedido.CANCELADO, cancelado.getEstado());
        verify(inventarioClient, times(1)).liberarStock(10L, 2);
        verify(pedidoRepository, times(1)).save(pedido);
    }

    @Test
    void cancelarPedido_usuarioIncorrecto() {
        // Arrange
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuarioId(2L); // Diferente
        when(pedidoRepository.findById(1L)).thenReturn(java.util.Optional.of(pedido));

        // Act & Assert
        assertThrows(io.github.danielmelejpinto.pedidoapi.exception.PedidoNoEncontradoException.class, 
            () -> pedidoService.cancelarPedido(1L, 1L));
    }

    @Test
    void cancelarPedido_estadoInvalido() {
        // Arrange
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuarioId(1L);
        pedido.setEstado(EstadoPedido.CANCELADO); // Ya está cancelado
        when(pedidoRepository.findById(1L)).thenReturn(java.util.Optional.of(pedido));

        // Act & Assert
        assertThrows(io.github.danielmelejpinto.pedidoapi.exception.EstadoPedidoInvalidoException.class, 
            () -> pedidoService.cancelarPedido(1L, 1L));
    }
}