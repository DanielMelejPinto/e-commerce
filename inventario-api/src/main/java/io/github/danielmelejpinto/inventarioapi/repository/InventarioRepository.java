package io.github.danielmelejpinto.inventarioapi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.danielmelejpinto.inventarioapi.model.Inventario;

public interface InventarioRepository extends JpaRepository<Inventario, Long>{
    
    Optional<Inventario> findByProductoId(Long productoID);
}
