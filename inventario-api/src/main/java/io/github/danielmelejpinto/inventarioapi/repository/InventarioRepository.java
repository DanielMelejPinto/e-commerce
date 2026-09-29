package io.github.danielmelejpinto.inventarioapi.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.danielmelejpinto.inventarioapi.model.Inventario;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    Optional<Inventario> findByProductoId(Long productoId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Inventario i SET i.cantidadDisponible = i.cantidadDisponible - :cantidad, i.cantidadReservada = i.cantidadReservada + :cantidad, i.ultimaActualizacion = CURRENT_TIMESTAMP, i.version = i.version + 1 WHERE i.productoId = :productoId AND i.cantidadDisponible >= :cantidad")
    int reservarStockAtomico(@Param("productoId") Long productoId, @Param("cantidad") long cantidad);
}
