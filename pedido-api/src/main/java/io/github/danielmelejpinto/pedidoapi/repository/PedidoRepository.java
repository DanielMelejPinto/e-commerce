package io.github.danielmelejpinto.pedidoapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.danielmelejpinto.pedidoapi.model.Pedido;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    
}
