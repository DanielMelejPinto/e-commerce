package io.github.danielmelejpinto.pedidoapi.repository;

import io.github.danielmelejpinto.pedidoapi.model.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @EntityGraph(attributePaths = "items")
    List<Pedido> findByUsuarioId(Long usuarioId);

    @Override
    @EntityGraph(attributePaths = "items")
    Optional<Pedido> findById(Long id);
}
