package io.github.danielmelejpinto.pedidoapi.controller;

import io.github.danielmelejpinto.pedidoapi.dto.PedidoItemRequest;
import io.github.danielmelejpinto.pedidoapi.dto.PedidoRequest;
import io.github.danielmelejpinto.pedidoapi.dto.PedidoResponse;
import io.github.danielmelejpinto.pedidoapi.model.EstadoPedido;
import io.github.danielmelejpinto.pedidoapi.model.Pedido;
import io.github.danielmelejpinto.pedidoapi.service.PedidoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    @Mock
    private PedidoService pedidoService;

    @InjectMocks
    private PedidoController pedidoController;

    @Test
    void crearPedido_valido_retorna201() {
        // Arrange
        PedidoItemRequest itemReq = new PedidoItemRequest(1L, 2);
        // Ya no enviamos el usuarioId en el JSON
        PedidoRequest request = new PedidoRequest(List.of(itemReq));
        Long usuarioId = 10L;

        Pedido pedidoMock = new Pedido();
        pedidoMock.setId(100L);
        pedidoMock.setUsuarioId(usuarioId);
        pedidoMock.setEstado(EstadoPedido.CONFIRMADO);
        pedidoMock.setTotal(new BigDecimal("200.00"));
        pedidoMock.setFechaCreacion(LocalDateTime.now());

        // El mock ahora espera el usuarioId suelto
        when(pedidoService.crearPedido(eq(usuarioId), any(PedidoRequest.class))).thenReturn(pedidoMock);

        // Act
        ResponseEntity<PedidoResponse> response = pedidoController.crearPedido(usuarioId, request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(100L, response.getBody().id());
        assertEquals("CONFIRMADO", response.getBody().estado());
    }

    @Test
    void cancelarPedido_valido_retorna200() {
        // Arrange
        Long usuarioId = 1L;
        Long pedidoId = 100L;
        
        Pedido pedidoMock = new Pedido();
        pedidoMock.setId(pedidoId);
        pedidoMock.setUsuarioId(usuarioId);
        pedidoMock.setEstado(EstadoPedido.CANCELADO);
        pedidoMock.setTotal(new BigDecimal("200.00"));
        pedidoMock.setFechaCreacion(LocalDateTime.now());

        when(pedidoService.cancelarPedido(usuarioId, pedidoId)).thenReturn(pedidoMock);

        // Act
        ResponseEntity<PedidoResponse> response = pedidoController.cancelarPedido(usuarioId, pedidoId);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pedidoId, response.getBody().id());
        assertEquals("CANCELADO", response.getBody().estado());
    }
}