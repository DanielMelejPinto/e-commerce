package io.github.danielmelejpinto.productoapi.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.danielmelejpinto.productoapi.model.EstadoEvento;
import io.github.danielmelejpinto.productoapi.model.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByEstado(EstadoEvento estado);
}
