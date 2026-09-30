package io.github.danielmelejpinto.pedidoapi.repository;

import io.github.danielmelejpinto.pedidoapi.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByUsuarioId(Long usuarioId);
}
