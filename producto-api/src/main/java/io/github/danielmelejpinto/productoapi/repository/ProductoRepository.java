package io.github.danielmelejpinto.productoapi.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import io.github.danielmelejpinto.productoapi.model.EstadoProducto;
import io.github.danielmelejpinto.productoapi.model.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    Page<Producto> findByEstado(EstadoProducto estado, Pageable pageable);
    Page<Producto> findByEstadoAndNombreContainingIgnoreCase(EstadoProducto estado, String nombre, Pageable pageable);
}
