package io.github.danielmelejpinto.pedido.service;

import io.github.danielmelejpinto.pedido.client.InventarioClient;
import io.github.danielmelejpinto.pedido.client.ProductoClient;
import io.github.danielmelejpinto.pedido.client.dto.ProductoDTO;
import io.github.danielmelejpinto.pedido.dto.PedidoItemRequest;
import io.github.danielmelejpinto.pedido.dto.PedidoRequest;
import io.github.danielmelejpinto.pedido.model.EstadoPedido;
import io.github.danielmelejpinto.pedido.model.Pedido;
import io.github.danielmelejpinto.pedido.repository.PedidoRepository;
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
        PedidoRequest request = new PedidoRequest(usuarioId, List.of(itemRequest));

        ProductoDTO productoDTO = new ProductoDTO(productoId, "Producto Test", precio, "ACTIVO");

        when(productoClient.obtenerProducto(productoId)).thenReturn(productoDTO);
        doNothing().when(inventarioClient).reservarStock(productoId, cantidad);
        
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(1L); // Simulamos que la BD le asigna un ID
            return p;
        });

        // Act
        Pedido pedidoCreado = pedidoService.crearPedido(request);

        // Assert
        assertNotNull(pedidoCreado);
        assertEquals(usuarioId, pedidoCreado.getUsuarioId());
        assertEquals(EstadoPedido.CONFIRMADO, pedidoCreado.getEstado());
        assertEquals(new BigDecimal("100.00"), pedidoCreado.getTotal()); // 50.00 * 2
        assertEquals(1, pedidoCreado.getItems().size());
        
        verify(productoClient, times(1)).obtenerProducto(productoId);
        verify(inventarioClient, times(1)).reservarStock(productoId, cantidad);
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
    }
}
