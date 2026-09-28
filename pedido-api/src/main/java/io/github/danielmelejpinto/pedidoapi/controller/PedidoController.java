package io.github.danielmelejpinto.pedidoapi.controller;

// Importamos las nuevas herramientas para los códigos HTTP
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

import io.github.danielmelejpinto.pedidoapi.model.Pedido;
import io.github.danielmelejpinto.pedidoapi.service.PedidoService;

@RestController
@RequestMapping("/api/pedido")
public class PedidoController {
   
    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }
    
    @PostMapping
    public ResponseEntity<Pedido> crear(@RequestBody Pedido pedido) {
        Pedido nuevoPedido = service.crearPedido(pedido);
        // Retornamos 201 Created
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoPedido);
    }

    @GetMapping
    public ResponseEntity<List<Pedido>> listarTodos() {
        // ResponseEntity.ok() es un atajo elegante para devolver 200 OK
        return ResponseEntity.ok(service.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Pedido> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Pedido> actualizarPedido(@PathVariable Long id, @RequestBody Pedido pedidoActualizado) {
        return ResponseEntity.ok(service.actualizarPedido(id, pedidoActualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPedido(@PathVariable Long id) {
        service.eliminarPedido(id);
        // Retornamos 204 No Content (Éxito, pero sin datos de vuelta)
        return ResponseEntity.noContent().build();
    }
}