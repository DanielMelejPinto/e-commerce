package io.github.danielmelejpinto.inventarioapi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.danielmelejpinto.inventarioapi.model.Reserva;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    Optional<Reserva> findByPedidoIdAndProductoId(Long pedidoId, Long productoId);
}
