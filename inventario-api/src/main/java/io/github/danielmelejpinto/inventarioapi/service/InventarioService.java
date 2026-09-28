package io.github.danielmelejpinto.inventarioapi.service;

import org.springframework.stereotype.Service;
import io.github.danielmelejpinto.inventarioapi.model.Inventario;
import io.github.danielmelejpinto.inventarioapi.repository.InventarioRepository;
import java.time.LocalDateTime;

@Service
public class InventarioService {

    // Inyección de dependencias mediante constructor (Buenas prácticas)
    private final InventarioRepository repository;

    public InventarioService(InventarioRepository repository) {
        this.repository = repository;
    }

    // 1. Obtener el inventario de un producto específico
    public Inventario obtenerPorProductoId(Long productoId) {
        return repository.findByProductoId(productoId)
                .orElseThrow(() -> new RuntimeException("No se encontró inventario para el producto: " + productoId));
    }

    // 2. Crear un registro de inventario desde cero (Ej: al registrar un producto nuevo)
    public Inventario inicializarInventario(Long productoId) {
        Inventario inventario = new Inventario();
        inventario.setProductoId(productoId);
        inventario.setCantidadDisponible(0);
        inventario.setCantidadReservada(0);
        inventario.setUltimaActualizacion(LocalDateTime.now());
        
        return repository.save(inventario);
    }

    // 3. Agregar stock (Ej: cuando llegan nuevas unidades a la bodega)
    public Inventario agregarStock(Long productoId, Integer cantidadAñadida) {
        Inventario inventario = obtenerPorProductoId(productoId);
        
        int nuevoStock = inventario.getCantidadDisponible() + cantidadAñadida;
        inventario.setCantidadDisponible(nuevoStock);
        inventario.setUltimaActualizacion(LocalDateTime.now());
        
        return repository.save(inventario);
    }

    // 4. Reservar stock (Ej: cuando el cliente le da a "Comprar")
    public void reservarStock(Long productoId, Integer cantidadComprada) {
        Inventario inventario = obtenerPorProductoId(productoId);
        
        // Regla de negocio: ¿Hay suficiente stock para vender?
        if (inventario.getCantidadDisponible() >= cantidadComprada) {
            
            // Restamos de disponible y pasamos a reservado
            inventario.setCantidadDisponible(inventario.getCantidadDisponible() - cantidadComprada);
            inventario.setCantidadReservada(inventario.getCantidadReservada() + cantidadComprada);
            inventario.setUltimaActualizacion(LocalDateTime.now());
            
            repository.save(inventario);
        } else {
            // Si no alcanza, detenemos la operación con un error
            throw new RuntimeException("Stock insuficiente para el producto: " + productoId);
        }
    }
}