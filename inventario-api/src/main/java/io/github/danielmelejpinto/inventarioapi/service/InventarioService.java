package io.github.danielmelejpinto.inventarioapi.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.danielmelejpinto.inventarioapi.dto.InventarioResponse;
import io.github.danielmelejpinto.inventarioapi.exception.InventarioNoEncontradoException;
import io.github.danielmelejpinto.inventarioapi.exception.StockInsuficienteException;
import io.github.danielmelejpinto.inventarioapi.model.Inventario;
import io.github.danielmelejpinto.inventarioapi.repository.InventarioRepository;

@Service
@Transactional(readOnly = true)
public class InventarioService {

    private final InventarioRepository repository;

    public InventarioService(InventarioRepository repository) {
        this.repository = repository;
    }

    public InventarioResponse obtenerPorProductoId(Long productoId) {
        return mapearAResponse(buscar(productoId));
    }

    // Idempotente: si ya existe, devuelve el existente en vez de crear un duplicado
    @Transactional
    public InventarioResponse inicializarInventario(Long productoId) {
        Inventario inventario = repository.findByProductoId(productoId)
                .orElseGet(() -> repository.save(crearVacio(productoId)));
        return mapearAResponse(inventario);
    }

    // No hace falta repository.save(): la entidad está gestionada por JPA y se
    // persiste al cerrar la transacción (con la comprobación de @Version)
    @Transactional
    public InventarioResponse agregarStock(Long productoId, int cantidad) {
        Inventario inventario = buscar(productoId);
        inventario.setCantidadDisponible(inventario.getCantidadDisponible() + cantidad);
        inventario.setUltimaActualizacion(LocalDateTime.now());
        return mapearAResponse(inventario);
    }

    @Transactional
    public InventarioResponse reservarStock(Long productoId, int cantidad) {
        Inventario inventario = buscar(productoId);

        if (inventario.getCantidadDisponible() < cantidad) {
            throw new StockInsuficienteException(productoId, inventario.getCantidadDisponible(), cantidad);
        }

        inventario.setCantidadDisponible(inventario.getCantidadDisponible() - cantidad);
        inventario.setCantidadReservada(inventario.getCantidadReservada() + cantidad);
        inventario.setUltimaActualizacion(LocalDateTime.now());
        return mapearAResponse(inventario);
    }

    @Transactional
    public void eliminarInventario(Long productoId) {
        repository.findByProductoId(productoId).ifPresent(repository::delete);
    }

    private Inventario buscar(Long productoId) {
        return repository.findByProductoId(productoId)
                .orElseThrow(() -> new InventarioNoEncontradoException(productoId));
    }

    private Inventario crearVacio(Long productoId) {
        Inventario inventario = new Inventario();
        inventario.setProductoId(productoId);
        inventario.setCantidadDisponible(0L);
        inventario.setCantidadReservada(0L);
        inventario.setUltimaActualizacion(LocalDateTime.now());
        return inventario;
    }

    private InventarioResponse mapearAResponse(Inventario inventario) {
        return new InventarioResponse(
            inventario.getProductoId(),
            inventario.getCantidadDisponible(),
            inventario.getCantidadReservada(),
            inventario.getUltimaActualizacion()
        );
    }
}