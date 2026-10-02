package io.github.danielmelejpinto.pedidoapi.service;

import io.github.danielmelejpinto.pedidoapi.client.InventarioClient;
import io.github.danielmelejpinto.pedidoapi.client.ProductoClient;
import io.github.danielmelejpinto.pedidoapi.client.dto.ProductoDTO;
import io.github.danielmelejpinto.pedidoapi.dto.PedidoItemRequest;
import io.github.danielmelejpinto.pedidoapi.dto.PedidoRequest;
import io.github.danielmelejpinto.pedidoapi.exception.EstadoPedidoInvalidoException;
import io.github.danielmelejpinto.pedidoapi.exception.PedidoNoEncontradoException;
import io.github.danielmelejpinto.pedidoapi.exception.StockInsuficienteException;
import io.github.danielmelejpinto.pedidoapi.model.ClaveIdempotencia;
import io.github.danielmelejpinto.pedidoapi.model.ClaveIdempotenciaId;
import io.github.danielmelejpinto.pedidoapi.model.EstadoPedido;
import io.github.danielmelejpinto.pedidoapi.model.Pedido;
import io.github.danielmelejpinto.pedidoapi.model.PedidoItem;
import io.github.danielmelejpinto.pedidoapi.repository.ClaveIdempotenciaRepository;
import io.github.danielmelejpinto.pedidoapi.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    // SHA-256 de "10:1;" (productoId:cantidad;), que es lo que genera PedidoService.generarHash
    private static final String HASH_PEDIDO_10_X1 =
            "8a4082c0d7fe84ad455c2b82ce6e1fdf111c159a3225011dcb924a1e47e79989";

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private ProductoClient productoClient;

    @Mock
    private InventarioClient inventarioClient;

    @Mock
    private ClaveIdempotenciaRepository claveIdempotenciaRepository;

    @Mock
    private ApplicationContext context;

    @InjectMocks
    private PedidoService pedidoService;

    private final AtomicReference<Pedido> pedidoGuardado = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        // En produccion PedidoService se llama a si mismo a traves del proxy de Spring (para que
        // @Transactional se aplique). En el test, el "proxy" es la propia instancia.
        lenient().when(context.getBean(PedidoService.class)).thenReturn(pedidoService);
    }

    /** Simula la persistencia: saveAndFlush asigna el id y findById/save devuelven ese mismo pedido. */
    private void simularPersistencia(Long idAsignado) {
        lenient().when(pedidoRepository.saveAndFlush(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(idAsignado);
            pedidoGuardado.set(p);
            return p;
        });
        lenient().when(pedidoRepository.findById(idAsignado))
                .thenAnswer(invocation -> Optional.ofNullable(pedidoGuardado.get()));
        lenient().when(pedidoRepository.save(any(Pedido.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void crearPedido_exitoso() {
        // Arrange
        Long usuarioId = 1L;
        Long productoId = 100L;
        Integer cantidad = 2;
        BigDecimal precio = new BigDecimal("50.00");

        PedidoRequest request = new PedidoRequest(List.of(new PedidoItemRequest(productoId, cantidad)));
        when(productoClient.obtenerProducto(productoId))
                .thenReturn(new ProductoDTO(productoId, "Producto Test", precio, "ACTIVO"));
        simularPersistencia(1L);

        // Act
        Pedido pedidoCreado = pedidoService.crearPedido(usuarioId, request, null);

        // Assert
        assertNotNull(pedidoCreado);
        assertEquals(usuarioId, pedidoCreado.getUsuarioId());
        assertEquals(EstadoPedido.CONFIRMADO, pedidoCreado.getEstado());
        assertEquals(new BigDecimal("100.00"), pedidoCreado.getTotal());
        assertEquals(1, pedidoCreado.getItems().size());
        assertEquals(productoId, pedidoCreado.getItems().get(0).getProductoId());

        verify(productoClient, times(1)).obtenerProducto(productoId);
        verify(inventarioClient, times(1)).reservarStock(productoId, cantidad, 1L);
        verify(pedidoRepository, times(1)).saveAndFlush(any(Pedido.class));
    }

    @Test
    void crearPedido_idempotenciaYaExiste_retornaPedidoExistente() {
        // Arrange
        String idempotencyKey = "test-uuid";
        Long usuarioId = 1L;
        Long pedidoExistenteId = 55L;
        PedidoRequest request = new PedidoRequest(List.of(new PedidoItemRequest(10L, 1)));

        ClaveIdempotencia clave = new ClaveIdempotencia(
                usuarioId, idempotencyKey, pedidoExistenteId, HASH_PEDIDO_10_X1, "COMPLETADO");
        when(claveIdempotenciaRepository.findById(new ClaveIdempotenciaId(usuarioId, idempotencyKey)))
                .thenReturn(Optional.of(clave));

        Pedido pedidoExistente = new Pedido();
        pedidoExistente.setId(pedidoExistenteId);
        pedidoExistente.setEstado(EstadoPedido.CONFIRMADO);
        when(pedidoRepository.findById(pedidoExistenteId)).thenReturn(Optional.of(pedidoExistente));

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
        Long usuarioId2 = 2L;
        PedidoRequest request = new PedidoRequest(List.of(new PedidoItemRequest(10L, 1)));

        when(productoClient.obtenerProducto(10L))
                .thenReturn(new ProductoDTO(10L, "Producto Test", new BigDecimal("50.00"), "ACTIVO"));

        // La clave se busca por (usuarioId, clave): para el usuario 2 no existe
        when(claveIdempotenciaRepository.findById(new ClaveIdempotenciaId(usuarioId2, idempotencyKey)))
                .thenReturn(Optional.empty());
        simularPersistencia(99L);

        // Act
        Pedido pedidoCreado = pedidoService.crearPedido(usuarioId2, request, idempotencyKey);

        // Assert
        assertEquals(99L, pedidoCreado.getId());
        assertEquals(usuarioId2, pedidoCreado.getUsuarioId());

        verify(productoClient, times(1)).obtenerProducto(10L);
        verify(inventarioClient, times(1)).reservarStock(10L, 1, 99L);
        verify(pedidoRepository, times(1)).saveAndFlush(any(Pedido.class));
        verify(claveIdempotenciaRepository, times(1)).saveAndFlush(any());
    }

    @Test
    void crearPedido_fallaGuardadoClaveIdempotencia_noReservaStock() {
        // Arrange: la clave se guarda ANTES de reservar, asi que si falla no hay nada que compensar
        PedidoRequest request = new PedidoRequest(List.of(new PedidoItemRequest(100L, 2)));
        simularPersistencia(1L);
        when(claveIdempotenciaRepository.saveAndFlush(any()))
                .thenThrow(new RuntimeException("Error BD clave idempotencia simulado"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> pedidoService.crearPedido(1L, request, "uuid-123"));

        verify(inventarioClient, never()).reservarStock(any(), any(), any());
        verify(inventarioClient, never()).liberarStock(any(), any(), any());
    }

    @Test
    void crearPedido_fallaReserva_liberaStockYPropagaError() {
        // Arrange
        Long productoId = 100L;
        Integer cantidad = 2;
        PedidoRequest request = new PedidoRequest(List.of(new PedidoItemRequest(productoId, cantidad)));

        when(productoClient.obtenerProducto(productoId))
                .thenReturn(new ProductoDTO(productoId, "Producto Test", new BigDecimal("50.00"), "ACTIVO"));
        simularPersistencia(1L);
        doThrow(new StockInsuficienteException("Stock insuficiente"))
                .when(inventarioClient).reservarStock(productoId, cantidad, 1L);

        // Act & Assert
        assertThrows(StockInsuficienteException.class, () -> pedidoService.crearPedido(1L, request, null));

        // La saga compensa: libera lo que pudiera haberse reservado para este pedido
        verify(inventarioClient, times(1)).liberarStock(productoId, cantidad, 1L);
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
        PedidoItem item = new PedidoItem();
        item.setProductoId(10L);
        item.setCantidad(2);
        pedido.addItem(item);

        when(pedidoRepository.findById(pedidoId)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.cancelarSiConfirmado(pedidoId, usuarioId)).thenReturn(1);

        // Act
        Pedido cancelado = pedidoService.cancelarPedido(usuarioId, pedidoId);

        // Assert
        assertEquals(EstadoPedido.CANCELADO, cancelado.getEstado());
        verify(inventarioClient, times(1)).liberarStock(10L, 2, 1L);
        verify(pedidoRepository, times(1)).cancelarSiConfirmado(pedidoId, usuarioId);
        verify(pedidoRepository, never()).save(any(Pedido.class));
    }

    @Test
    void cancelarPedido_usuarioIncorrecto() {
        // Arrange
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuarioId(2L); // Diferente
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        // Act & Assert
        assertThrows(PedidoNoEncontradoException.class, () -> pedidoService.cancelarPedido(1L, 1L));
    }

    @Test
    void cancelarPedido_estadoInvalido() {
        // Arrange
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuarioId(1L);
        pedido.setEstado(EstadoPedido.CANCELADO); // Ya esta cancelado
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        // Act & Assert
        assertThrows(EstadoPedidoInvalidoException.class, () -> pedidoService.cancelarPedido(1L, 1L));
        verify(inventarioClient, never()).liberarStock(any(), any(), any());
    }

    @Test
    void cancelarPedido_sinCambiarEstado_lanzaErrorSinDobleLiberacion() {
        // Arrange: el pedido se ve CONFIRMADO al leerlo, pero otra cancelacion gano la carrera
        // y el UPDATE condicional no cambia ninguna fila. Liberar en inventario es idempotente
        // (reserva unica por pedido y producto), por eso llamarlo dos veces no libera stock doble.
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuarioId(1L);
        pedido.setEstado(EstadoPedido.CONFIRMADO);
        PedidoItem item = new PedidoItem();
        item.setProductoId(10L);
        item.setCantidad(2);
        pedido.addItem(item);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.cancelarSiConfirmado(1L, 1L)).thenReturn(0);

        // Act & Assert
        assertThrows(EstadoPedidoInvalidoException.class, () -> pedidoService.cancelarPedido(1L, 1L));
        verify(inventarioClient, times(1)).liberarStock(10L, 2, 1L);
    }
}
