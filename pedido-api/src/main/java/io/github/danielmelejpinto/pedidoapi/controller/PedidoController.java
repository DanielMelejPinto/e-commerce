package io.github.danielmelejpinto.pedidoapi.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;

import io.github.danielmelejpinto.pedidoapi.model.Pedido;
import io.github.danielmelejpinto.pedidoapi.service.PedidoService;

@RestController
@RequestMapping("/api/pedido") // Definimos la ruta base para todo el controlador
public class PedidoController {
   
    // 1. Inyectamos nuestro servicio (usando el Constructor como aprendiste)
    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    // 2. Endpoint de prueba original
    @GetMapping("/estado")
    public String estado() {
        return "API de pedido funcionando";
    }
    
    // 3. NUEVO: Endpoint para crear un pedido
    @PostMapping
    public Pedido crear(@RequestBody Pedido pedido) {
        // @RequestBody agarra el JSON que envía el cliente y lo convierte a objeto Pedido
        return service.crearPedido(pedido);
    }

    // 4. NUEVO: Endpoint para listar todos los pedidos
    @GetMapping
    public List<Pedido> listarTodos() {
        return service.obtenerTodos();
    }
}