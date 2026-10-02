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
import io.github.danielmelejpinto.inventarioapi.repository.ReservaRepository;
import io.github.danielmelejpinto.inventarioapi.model.Reserva;

@Service
@Transactional(readOnly = true)
public class InventarioService {

    private final InventarioRepository repository;
    private final ReservaRepository reservaRepository;

    public InventarioService(InventarioRepository repository, ReservaRepository reservaRepository) {
        this.repository = repository;
        this.reservaRepository = reservaRepository;
    }

    public InventarioResponse obtenerPorProductoId(Long productoId) {
        return mapearAResponse(buscar(productoId));
    }

    @Transactional(propagation = Propagation.NEVER)
    public ResultadoInicializacion inicializarInventario(Long productoId) {
        return repository.findByProductoId(productoId)
                .map(inv -> new ResultadoInicializacion(mapearAResponse(inv), false))
                .orElseGet(() -> {
                    try {
                        Inventario nuevo = repository.save(crearVacio(productoId));
                        return new ResultadoInicializacion(mapearAResponse(nuevo), true);
                    } catch (DataIntegrityViolationException e) {
                        Inventario existente = repository.findByProductoId(productoId)
                                .orElseThrow(() -> new IllegalStateException(
                                        "Se esperaba encontrar el inventario tras colisión, pero no está", e));
                        return new ResultadoInicializacion(mapearAResponse(existente), false);
                    }
                });
    }

    @Transactional
    public InventarioResponse agregarStock(Long productoId, int cantidad) {
        Inventario inventario = buscar(productoId);
        inventario.setCantidadDisponible(inventario.getCantidadDisponible() + cantidad);
        inventario.setUltimaActualizacion(LocalDateTime.now());
        return mapearAResponse(inventario);
    }

    @Transactional
    public InventarioResponse reservarStock(Long productoId, int cantidad, Long pedidoId) {
        if (reservaRepository.findByPedidoIdAndProductoId(pedidoId, productoId).isPresent()) {
            return mapearAResponse(buscar(productoId));
        }

        int filasActualizadas = repository.reservarStockAtomico(productoId, cantidad);
        if (filasActualizadas == 0) {
            Inventario inventario = buscar(productoId);
            throw new StockInsuficienteException(productoId, inventario.getCantidadDisponible(), cantidad);
        }

        reservaRepository.save(new Reserva(pedidoId, productoId, cantidad));
        return mapearAResponse(buscar(productoId));
    }

    @Transactional
    public void eliminarInventario(Long productoId) {
        // Idempotente: si no existe, no hace nada (204). Si tiene reservas activas, no se borra.
        repository.findByProductoId(productoId).ifPresent(inventario -> {
            if (inventario.getCantidadReservada() > 0) {
                throw new IllegalStateException("No se puede eliminar el inventario porque tiene reservas activas");
            }
            repository.delete(inventario);
        });
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
                inventario.getUltimaActualizacion());
    }

    @Transactional
    public InventarioResponse liberarStock(Long productoId, int cantidad, Long pedidoId) {
        var reservaOpt = reservaRepository.findByPedidoIdAndProductoId(pedidoId, productoId);
        if (reservaOpt.isEmpty()) {
            throw new IllegalStateException("No existe la reserva para el pedido y producto indicados");
        }
        
        Reserva reserva = reservaOpt.get();
        if ("LIBERADA".equals(reserva.getEstado())) {
            // Ya fue liberada: es idempotente
            return mapearAResponse(buscar(productoId));
        }

        reserva.setEstado("LIBERADA");
        reservaRepository.save(reserva);
        repository.liberarStockAtomico(productoId, reserva.getCantidad());
        
        return mapearAResponse(buscar(productoId));
    }
}
