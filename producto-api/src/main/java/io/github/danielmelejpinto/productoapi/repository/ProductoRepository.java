package io.github.danielmelejpinto.productoapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.danielmelejpinto.productoapi.model.Producto;


public interface ProductoRepository extends JpaRepository<Producto, Long> {
    
}
