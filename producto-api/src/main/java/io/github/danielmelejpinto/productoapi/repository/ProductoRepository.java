package io.github.danielmelejpinto.productoapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.danielmelejpinto.productoapi.model.Producto;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import io.github.danielmelejpinto.productoapi.model.EstadoProducto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    Page<Producto> findByEstado(EstadoProducto estado, Pageable pageable);
}
