package io.github.danielmelejpinto.pedidoapi.repository;

import io.github.danielmelejpinto.pedidoapi.model.EstadoPedido;
import io.github.danielmelejpinto.pedidoapi.model.Pedido;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
class PedidoRepositoryCancelarTest {

    @Autowired
    private PedidoRepository pedidoRepository;

    private Pedido guardar(Long usuarioId, EstadoPedido estado) {
        Pedido p = new Pedido();
        p.setUsuarioId(usuarioId);
        p.setEstado(estado);
        p.setTotal(new BigDecimal("100.00"));
        return pedidoRepository.saveAndFlush(p);
    }

    @Test
    void cancelarSiConfirmado_confirmadoDelDueno_devuelve1YCancela() {
        Pedido p = guardar(1L, EstadoPedido.CONFIRMADO);

        int filas = pedidoRepository.cancelarSiConfirmado(p.getId(), 1L);

        assertEquals(1, filas);
        assertEquals(EstadoPedido.CANCELADO, pedidoRepository.findById(p.getId()).orElseThrow().getEstado());
    }

    @Test
    void cancelarSiConfirmado_segundaLlamada_devuelve0() {
        Pedido p = guardar(1L, EstadoPedido.CONFIRMADO);

        assertEquals(1, pedidoRepository.cancelarSiConfirmado(p.getId(), 1L));
        assertEquals(0, pedidoRepository.cancelarSiConfirmado(p.getId(), 1L));
    }

    @Test
    void cancelarSiConfirmado_otroUsuario_devuelve0YNoCambia() {
        Pedido p = guardar(1L, EstadoPedido.CONFIRMADO);

        assertEquals(0, pedidoRepository.cancelarSiConfirmado(p.getId(), 2L));
        assertEquals(EstadoPedido.CONFIRMADO, pedidoRepository.findById(p.getId()).orElseThrow().getEstado());
    }

    @Test
    void cancelarSiConfirmado_pendiente_devuelve0() {
        Pedido p = guardar(1L, EstadoPedido.PENDIENTE);

        assertEquals(0, pedidoRepository.cancelarSiConfirmado(p.getId(), 1L));
    }
}
