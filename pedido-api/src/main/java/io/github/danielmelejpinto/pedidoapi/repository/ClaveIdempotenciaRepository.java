package io.github.danielmelejpinto.pedidoapi.repository;

import io.github.danielmelejpinto.pedidoapi.model.ClaveIdempotencia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaveIdempotenciaRepository extends JpaRepository<ClaveIdempotencia, String> {
}
