package io.github.danielmelejpinto.inventarioapi.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.github.danielmelejpinto.inventarioapi.dto.CantidadRequest;
import io.github.danielmelejpinto.inventarioapi.dto.InventarioResponse;
import io.github.danielmelejpinto.inventarioapi.service.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/inventarios")
@Tag(name = "Inventario", description = "Existencias y reservas de stock por producto")
public class InventarioController {

    private final InventarioService service;

    public InventarioController(InventarioService service) {
        this.service = service;
    }

    @GetMapping("/producto/{productoId}")
    @Operation(summary = "Consultar el inventario de un producto")
    @ApiResponse(responseCode = "200", description = "Inventario encontrado")
    @ApiResponse(responseCode = "404", description = "El producto no tiene inventario")
    public InventarioResponse obtener(@PathVariable Long productoId) {
        return service.obtenerPorProductoId(productoId);
    }

    @PostMapping("/producto/{productoId}")
    @Operation(summary = "Inicializar el inventario de un producto (idempotente)")
    @ApiResponse(responseCode = "201", description = "Inventario listo, con stock inicial en cero")
    public ResponseEntity<InventarioResponse> inicializar(@PathVariable Long productoId) {
        InventarioResponse response = service.inicializarInventario(productoId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/producto/{productoId}/agregar")
    @Operation(summary = "Agregar stock (ingreso de mercadería)")
    @ApiResponse(responseCode = "200", description = "Stock actualizado")
    @ApiResponse(responseCode = "400", description = "Cantidad inválida")
    @ApiResponse(responseCode = "404", description = "El producto no tiene inventario")
    public InventarioResponse agregar(@PathVariable Long productoId,
            @Valid @RequestBody CantidadRequest request) {
        return service.agregarStock(productoId, request.getCantidad());
    }

    @PutMapping("/producto/{productoId}/reservar")
    @Operation(summary = "Reservar stock para una compra")
    @ApiResponse(responseCode = "200", description = "Stock reservado")
    @ApiResponse(responseCode = "400", description = "Cantidad inválida")
    @ApiResponse(responseCode = "404", description = "El producto no tiene inventario")
    @ApiResponse(responseCode = "409", description = "Stock insuficiente o conflicto de concurrencia")
    public InventarioResponse reservar(@PathVariable Long productoId,
            @Valid @RequestBody CantidadRequest request) {
        return service.reservarStock(productoId, request.getCantidad());
    }
}