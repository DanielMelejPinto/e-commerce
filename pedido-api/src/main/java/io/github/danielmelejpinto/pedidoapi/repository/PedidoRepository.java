package io.github.danielmelejpinto.pedidoapi.repository;

import io.github.danielmelejpinto.pedidoapi.model.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @EntityGraph(attributePaths = "items")
    List<Pedido> findByUsuarioId(Long usuarioId);

    @Override
    @EntityGraph(attributePaths = "items")
    Optional<Pedido> findById(Long id);

    /**
     * Pasa el pedido a CANCELADO solo si sigue CONFIRMADO y es del usuario.
     * Devuelve las filas cambiadas (1 o 0). El lock de fila hace que una
     * segunda cancelación simultánea espere y reciba 0.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Pedido p SET p.estado = io.github.danielmelejpinto.pedidoapi.model.EstadoPedido.CANCELADO "
            + "WHERE p.id = :id AND p.usuarioId = :usuarioId "
            + "AND p.estado = io.github.danielmelejpinto.pedidoapi.model.EstadoPedido.CONFIRMADO")
    int cancelarSiConfirmado(@Param("id") Long id, @Param("usuarioId") Long usuarioId);
}
