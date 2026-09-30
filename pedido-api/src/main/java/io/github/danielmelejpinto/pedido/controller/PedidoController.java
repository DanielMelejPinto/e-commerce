package io.github.danielmelejpinto.pedido.controller;

import io.github.danielmelejpinto.pedido.dto.PedidoRequest;
import io.github.danielmelejpinto.pedido.dto.PedidoResponse;
import io.github.danielmelejpinto.pedido.model.Pedido;
import io.github.danielmelejpinto.pedido.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> crearPedido(
            @RequestHeader("X-Usuario-Id") Long usuarioId,
            @Valid @RequestBody PedidoRequest request) {
        
        // Le pasamos el usuarioId seguro (que vino por header) al servicio
        Pedido pedido = pedidoService.crearPedido(usuarioId, request);
        return new ResponseEntity<>(PedidoResponse.fromEntity(pedido), HttpStatus.CREATED);
    }
    @GetMapping("/mis-pedidos") // Cambiamos la ruta para que sea relativa al usuario que hace la petición
    public ResponseEntity<List<PedidoResponse>> obtenerMisPedidos(
            @RequestHeader("X-Usuario-Id") Long usuarioId) {
        
        List<Pedido> pedidos = pedidoService.obtenerPedidosPorUsuario(usuarioId);
        List<PedidoResponse> response = pedidos.stream()
                .map(PedidoResponse::fromEntity)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(response);
    }
}