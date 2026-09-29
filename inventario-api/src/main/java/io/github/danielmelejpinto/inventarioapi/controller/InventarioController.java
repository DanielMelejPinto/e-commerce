package io.github.danielmelejpinto.inventarioapi.controller;

import org.springframework.web.bind.annotation.*;
import io.github.danielmelejpinto.inventarioapi.model.Inventario;
import io.github.danielmelejpinto.inventarioapi.service.InventarioService;

@RestController 
@RequestMapping("/api/inventarios")
public class InventarioController {
    
    private final InventarioService service;

    public InventarioController(InventarioService service) {
        this.service = service;
    }

    // 1. Consultar el inventario de un producto (GET)
    @GetMapping("/producto/{productoId}")
    public Inventario obtenerInventario(@PathVariable Long productoId) {
        return service.obtenerPorProductoId(productoId);
    }

    // 2. Inicializar inventario (POST) - Para cuando creas un producto nuevo
    @PostMapping("/producto/{productoId}")
    public Inventario inicializarInventario(@PathVariable Long productoId) {
        return service.inicializarInventario(productoId);
    }

    // 3. Agregar stock (PUT) - Para cuando te llega mercancía nueva
    @PutMapping("/producto/{productoId}/agregar")
    public Inventario agregarStock(
            @PathVariable Long productoId, 
            @RequestParam Integer cantidad) {
        
        return service.agregarStock(productoId, cantidad);
    }

    // 4. Reservar stock (PUT) - Para cuando un cliente va a pagar su carrito
    @PutMapping("/producto/{productoId}/reservar")
    public String reservarStock(
            @PathVariable Long productoId, 
            @RequestParam Integer cantidad) {
        
        service.reservarStock(productoId, cantidad);
        
        // Retornamos un mensaje simple de texto para confirmar que funcionó
        return "Stock de " + cantidad + " unidades reservado con éxito para el producto: " + productoId;
    }
}