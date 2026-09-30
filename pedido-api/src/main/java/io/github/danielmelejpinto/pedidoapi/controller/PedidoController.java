package io.github.danielmelejpinto.pedidoapi.controller;

import io.github.danielmelejpinto.pedidoapi.dto.PedidoRequest;
import io.github.danielmelejpinto.pedidoapi.dto.PedidoResponse;
import io.github.danielmelejpinto.pedidoapi.model.Pedido;
import io.github.danielmelejpinto.pedidoapi.service.PedidoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

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
            @AuthenticationPrincipal Long usuarioId,
            @Valid @RequestBody PedidoRequest request) {
        
        // Le pasamos el usuarioId seguro (que vino por header) al servicio
        Pedido pedido = pedidoService.crearPedido(usuarioId, request);
        return new ResponseEntity<>(PedidoResponse.fromEntity(pedido), HttpStatus.CREATED);
    }
    @GetMapping("/mis-pedidos") // Cambiamos la ruta para que sea relativa al usuario que hace la petición
    public ResponseEntity<List<PedidoResponse>> obtenerMisPedidos(
            @AuthenticationPrincipal Long usuarioId) {
        
        List<Pedido> pedidos = pedidoService.obtenerPedidosPorUsuario(usuarioId);
        List<PedidoResponse> response = pedidos.stream()
                .map(PedidoResponse::fromEntity)
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<PedidoResponse> cancelarPedido(
            @AuthenticationPrincipal Long usuarioId,
            @PathVariable Long id) {
        Pedido pedido = pedidoService.cancelarPedido(usuarioId, id);
        return ResponseEntity.ok(PedidoResponse.fromEntity(pedido));
    }
}