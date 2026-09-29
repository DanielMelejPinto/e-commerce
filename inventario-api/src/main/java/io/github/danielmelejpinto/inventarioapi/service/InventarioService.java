package io.github.danielmelejpinto.inventarioapi.service;

import java.time.LocalDateTime;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import io.github.danielmelejpinto.inventarioapi.dto.InventarioResponse;
import io.github.danielmelejpinto.inventarioapi.dto.ResultadoInicializacion;
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

    // Se usa Propagation.NEVER para que la excepción de unicidad (DataIntegrityViolationException)
    // que ocurre en repository.save no marque la transacción externa como rollback-only.
    // El repositorio en sí mismo maneja sus propias transacciones internas para cada método.
    // IMPORTANTE: Este método no debe llamarse dentro de otra transacción para poder atrapar la DataIntegrityViolationException correctamente.
    // propagation = NEVER indica que este método no puede llamarse desde dentro de otra transacción, para que la captura del DataIntegrityViolationException funcione correctamente y aislarlo de la Tx superior.
    @Transactional(propagation = Propagation.NEVER)
    public ResultadoInicializacion inicializarInventario(Long productoId) {
        return repository.findByProductoId(productoId)
                .map(inv -> new ResultadoInicializacion(mapearAResponse(inv), false))
                .orElseGet(() -> {
                    try {
                        Inventario nuevo = repository.save(crearVacio(productoId));
                        return new ResultadoInicializacion(mapearAResponse(nuevo), true);
                    } catch (DataIntegrityViolationException e) {
                        // Otro hilo o proceso ganó la carrera y lo creó primero.
                        // Volvemos a buscarlo (ya debería estar)
                        Inventario existente = repository.findByProductoId(productoId)
                                .orElseThrow(() -> new IllegalStateException("Se esperaba encontrar el inventario tras colisión, pero no está", e));
                        return new ResultadoInicializacion(mapearAResponse(existente), false);
                    }
                });
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
        int filasActualizadas = repository.reservarStockAtomico(productoId, cantidad);
        if (filasActualizadas == 0) {
            // No se actualizó: o no existe o no hay stock
            Inventario inventario = buscar(productoId); // si no existe lanza InventarioNoEncontradoException
            throw new StockInsuficienteException(productoId, inventario.getCantidadDisponible(), cantidad);
        }
        return mapearAResponse(buscar(productoId));
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