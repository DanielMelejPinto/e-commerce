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

    @Mock
    private io.github.danielmelejpinto.pedidoapi.repository.ClaveIdempotenciaRepository claveIdempotenciaRepository;

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
        doNothing().when(inventarioClient).reservarStock(productoId, cantidad, null);
        
        when(pedidoRepository.saveAndFlush(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        // Act (Se le pasa el usuarioId como primer parámetro)
        Pedido pedidoCreado = pedidoService.crearPedido(usuarioId, request, null);

        // Assert
        assertNotNull(pedidoCreado);
        assertEquals(usuarioId, pedidoCreado.getUsuarioId());
        assertEquals(EstadoPedido.CONFIRMADO, pedidoCreado.getEstado());
        assertEquals(new BigDecimal("100.00"), pedidoCreado.getTotal());
        assertEquals(1, pedidoCreado.getItems().size());
        
        verify(productoClient, times(1)).obtenerProducto(productoId);
        verify(inventarioClient, times(1)).reservarStock(productoId, cantidad, null);
        verify(pedidoRepository, times(1)).saveAndFlush(any(Pedido.class));
    }

    @Test
    void crearPedido_idempotenciaYaExiste_retornaPedidoExistente() {
        // Arrange
        String idempotencyKey = "test-uuid";
        Long usuarioId = 1L;
        Long pedidoExistenteId = 55L;
        PedidoRequest request = new PedidoRequest(List.of(new PedidoItemRequest(10L, 1)));
        
        io.github.danielmelejpinto.pedidoapi.model.ClaveIdempotencia claveMock = 
            new io.github.danielmelejpinto.pedidoapi.model.ClaveIdempotencia(usuarioId, idempotencyKey, pedidoExistenteId, null, "COMPLETADO");
            
        when(claveIdempotenciaRepository.findById(new io.github.danielmelejpinto.pedidoapi.model.ClaveIdempotenciaId(usuarioId, idempotencyKey)))
            .thenReturn(java.util.Optional.of(claveMock));
        
        Pedido pedidoExistente = new Pedido();
        pedidoExistente.setId(pedidoExistenteId);
        pedidoExistente.setEstado(EstadoPedido.CONFIRMADO);
        when(pedidoRepository.findById(pedidoExistenteId)).thenReturn(java.util.Optional.of(pedidoExistente));
        
        // Act
        Pedido pedidoRecuperado = pedidoService.crearPedido(usuarioId, request, idempotencyKey);
        
        // Assert
        assertEquals(pedidoExistenteId, pedidoRecuperado.getId());
        verify(productoClient, never()).obtenerProducto(any());
        verify(inventarioClient, never()).reservarStock(any(), any(), any());
        verify(pedidoRepository, never()).saveAndFlush(any());
    }
    
    @Test
    void crearPedido_idempotenciaYaExistePeroParaOtroUsuario_creaNuevoPedido() {
        // Arrange
        String idempotencyKey = "test-uuid";
        Long usuarioId2 = 2L; // El nuevo atacante o reintento erróneo
        
        PedidoItemRequest itemRequest = new PedidoItemRequest(10L, 1);
        PedidoRequest request = new PedidoRequest(List.of(itemRequest));
        
        ProductoDTO productoDTO = new ProductoDTO(10L, "Producto Test", new BigDecimal("50.00"), "ACTIVO");
        when(productoClient.obtenerProducto(10L)).thenReturn(productoDTO);
        doNothing().when(inventarioClient).reservarStock(10L, 1, null);
        
        // Para usuarioId2 NO se encontrará la clave, porque la clave se busca por (usuarioId, idempotencyKey)
        when(claveIdempotenciaRepository.findById(new io.github.danielmelejpinto.pedidoapi.model.ClaveIdempotenciaId(usuarioId2, idempotencyKey)))
            .thenReturn(java.util.Optional.empty());
            
        when(pedidoRepository.saveAndFlush(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(99L); // Nuevo pedido
            return p;
        });
        
        // Act
        Pedido pedidoCreado = pedidoService.crearPedido(usuarioId2, request, idempotencyKey);
        
        // Assert
        assertEquals(99L, pedidoCreado.getId());
        assertEquals(usuarioId2, pedidoCreado.getUsuarioId());
        
        verify(productoClient, times(1)).obtenerProducto(10L);
        verify(inventarioClient, times(1)).reservarStock(10L, 1, null);
        verify(pedidoRepository, times(1)).saveAndFlush(any(Pedido.class));
        verify(claveIdempotenciaRepository, times(1)).saveAndFlush(any());
    }
    
    @Test
    void crearPedido_fallaGuardadoClaveIdempotencia_liberaStockReservado() {
        // Arrange
        Long usuarioId = 1L;
        Long productoId = 100L;
        Integer cantidad = 2;
        BigDecimal precio = new BigDecimal("50.00");

        PedidoRequest request = new PedidoRequest(List.of(new PedidoItemRequest(productoId, cantidad)));
        ProductoDTO productoDTO = new ProductoDTO(productoId, "Producto Test", precio, "ACTIVO");

        when(productoClient.obtenerProducto(productoId)).thenReturn(productoDTO);
        doNothing().when(inventarioClient).reservarStock(productoId, cantidad, null);
        
        when(pedidoRepository.saveAndFlush(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(1L);
            return p;
        });

        // Simulamos un fallo al guardar la clave de idempotencia
        when(claveIdempotenciaRepository.saveAndFlush(any())).thenThrow(new RuntimeException("Error BD clave idempotencia simulado"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            pedidoService.crearPedido(usuarioId, request, "uuid-123");
        });

        // Verificamos que SE HAYA llamado a liberarStock para compensar
        verify(inventarioClient, times(1)).liberarStock(productoId, cantidad, null);
    }

    @Test
    void crearPedido_fallaGuardadoBD_liberaStockReservado() {
        // Arrange
        Long usuarioId = 1L;
        Long productoId = 100L;
        Integer cantidad = 2;
        BigDecimal precio = new BigDecimal("50.00");

        PedidoRequest request = new PedidoRequest(List.of(new PedidoItemRequest(productoId, cantidad)));
        ProductoDTO productoDTO = new ProductoDTO(productoId, "Producto Test", precio, "ACTIVO");

        when(productoClient.obtenerProducto(productoId)).thenReturn(productoDTO);
        doNothing().when(inventarioClient).reservarStock(productoId, cantidad, null);
        
        // Simulamos un fallo en base de datos DESPUES de haber reservado
        when(pedidoRepository.saveAndFlush(any(Pedido.class))).thenThrow(new RuntimeException("Error BD simulado"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            pedidoService.crearPedido(usuarioId, request, "uuid-123");
        });

        // Verificamos que SE HAYA llamado a liberarStock para compensar la reserva huérfana
        verify(inventarioClient, times(1)).liberarStock(productoId, cantidad, null);
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
        when(pedidoRepository.cancelarSiConfirmado(pedidoId, usuarioId)).thenReturn(1);
        doNothing().when(inventarioClient).liberarStock(10L, 2, 1L);

        // Act
        Pedido cancelado = pedidoService.cancelarPedido(usuarioId, pedidoId);

        // Assert
        assertEquals(EstadoPedido.CANCELADO, cancelado.getEstado());
        verify(inventarioClient, times(1)).liberarStock(10L, 2, 1L);
        verify(pedidoRepository, times(1)).cancelarSiConfirmado(pedidoId, usuarioId);
        verify(pedidoRepository, org.mockito.Mockito.never()).save(any(Pedido.class));
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

    @Test
    void cancelarPedido_sinCambiarEstado_noLiberaStock() {
        // Arrange: el pedido se ve CONFIRMADO al leerlo, pero otra cancelación
        // ganó la carrera y el UPDATE condicional no cambia ninguna fila.
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuarioId(1L);
        pedido.setEstado(EstadoPedido.CONFIRMADO);
        io.github.danielmelejpinto.pedidoapi.model.PedidoItem item = new io.github.danielmelejpinto.pedidoapi.model.PedidoItem();
        item.setProductoId(10L);
        item.setCantidad(2);
        pedido.addItem(item);

        when(pedidoRepository.findById(1L)).thenReturn(java.util.Optional.of(pedido));
        when(pedidoRepository.cancelarSiConfirmado(1L, 1L)).thenReturn(0);

        // Act & Assert
        assertThrows(io.github.danielmelejpinto.pedidoapi.exception.EstadoPedidoInvalidoException.class,
            () -> pedidoService.cancelarPedido(1L, 1L));
        verify(inventarioClient, org.mockito.Mockito.never())
            .liberarStock(any(), any(), any());
    }
}
