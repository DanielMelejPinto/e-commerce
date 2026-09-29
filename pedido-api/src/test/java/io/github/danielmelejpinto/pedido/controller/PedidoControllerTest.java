package io.github.danielmelejpinto.pedido.controller;

import io.github.danielmelejpinto.pedido.dto.PedidoItemRequest;
import io.github.danielmelejpinto.pedido.dto.PedidoRequest;
import io.github.danielmelejpinto.pedido.model.EstadoPedido;
import io.github.danielmelejpinto.pedido.model.Pedido;
import io.github.danielmelejpinto.pedido.service.PedidoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
        PedidoRequest request = new PedidoRequest(10L, List.of(itemReq));

        Pedido pedidoMock = new Pedido();
        pedidoMock.setId(100L);
        pedidoMock.setUsuarioId(10L);
        pedidoMock.setEstado(EstadoPedido.CONFIRMADO);
        pedidoMock.setTotal(new BigDecimal("200.00"));

        when(pedidoService.crearPedido(any(PedidoRequest.class))).thenReturn(pedidoMock);

        // Act
        ResponseEntity<Pedido> response = pedidoController.crearPedido(request);

        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(100L, response.getBody().getId());
        assertEquals(EstadoPedido.CONFIRMADO, response.getBody().getEstado());
    }
}
