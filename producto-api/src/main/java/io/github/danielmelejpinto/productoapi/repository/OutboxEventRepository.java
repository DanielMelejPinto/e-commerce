package io.github.danielmelejpinto.productoapi.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.danielmelejpinto.productoapi.model.EstadoEvento;
import io.github.danielmelejpinto.productoapi.model.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    
    @Query("SELECT o FROM OutboxEvent o WHERE o.estado = :estado AND (o.proximoReintento IS NULL OR o.proximoReintento <= :now)")
    List<OutboxEvent> findPendingEvents(@Param("estado") EstadoEvento estado, @Param("now") LocalDateTime now);
}
