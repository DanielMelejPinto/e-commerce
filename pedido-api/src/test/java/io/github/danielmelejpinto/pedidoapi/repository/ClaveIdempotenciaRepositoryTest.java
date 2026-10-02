package io.github.danielmelejpinto.pedidoapi.repository;

import io.github.danielmelejpinto.pedidoapi.model.ClaveIdempotencia;
import io.github.danielmelejpinto.pedidoapi.model.ClaveIdempotenciaId;
import io.github.danielmelejpinto.pedidoapi.model.EstadoPedido;
import io.github.danielmelejpinto.pedidoapi.model.Pedido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ClaveIdempotenciaRepositoryTest {

    @Autowired
    private ClaveIdempotenciaRepository claveIdempotenciaRepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Test
    void guardarYRecuperarClaveConPKCompuesta() {
        // Arrange
        Pedido pedido1 = new Pedido();
        pedido1.setUsuarioId(1L);
        pedido1.setTotal(new BigDecimal("100"));
        pedido1.setEstado(EstadoPedido.CONFIRMADO);
        pedidoRepository.saveAndFlush(pedido1);

        Pedido pedido2 = new Pedido();
        pedido2.setUsuarioId(2L);
        pedido2.setTotal(new BigDecimal("200"));
        pedido2.setEstado(EstadoPedido.CONFIRMADO);
        pedidoRepository.saveAndFlush(pedido2);

        String claveUUID = "clave-compartida";

        // Act - Guardamos para usuario 1
        ClaveIdempotencia clave1 = new ClaveIdempotencia(1L, claveUUID, pedido1.getId(), null, "COMPLETADO");
        claveIdempotenciaRepository.saveAndFlush(clave1);

        // Act - Guardamos misma clave pero para usuario 2
        ClaveIdempotencia clave2 = new ClaveIdempotencia(2L, claveUUID, pedido2.getId(), null, "COMPLETADO");
        claveIdempotenciaRepository.saveAndFlush(clave2);

        // Assert
        Optional<ClaveIdempotencia> recuperada1 = claveIdempotenciaRepository.findById(new ClaveIdempotenciaId(1L, claveUUID));
        assertTrue(recuperada1.isPresent());
        assertEquals(pedido1.getId(), recuperada1.get().getPedidoId());

        Optional<ClaveIdempotencia> recuperada2 = claveIdempotenciaRepository.findById(new ClaveIdempotenciaId(2L, claveUUID));
        assertTrue(recuperada2.isPresent());
        assertEquals(pedido2.getId(), recuperada2.get().getPedidoId());
    }
}
