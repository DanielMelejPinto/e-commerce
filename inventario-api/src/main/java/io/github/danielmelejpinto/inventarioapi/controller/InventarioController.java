package io.github.danielmelejpinto.inventarioapi.controller;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.github.danielmelejpinto.inventarioapi.dto.CantidadRequest;
import io.github.danielmelejpinto.inventarioapi.dto.InventarioResponse;
import io.github.danielmelejpinto.inventarioapi.dto.ResultadoInicializacion;
import io.github.danielmelejpinto.inventarioapi.service.InventarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/inventarios/producto/{productoId}")
@Tag(name = "Inventario", description = "Existencias y reservas de stock por producto")
public class InventarioController {

    private final InventarioService service;

    public InventarioController(InventarioService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Consultar el inventario de un producto")
    @ApiResponse(responseCode = "200", description = "Inventario encontrado")
    @ApiResponse(responseCode = "404", description = "El producto no tiene inventario")
    public InventarioResponse obtener(@PathVariable Long productoId) {
        return service.obtenerPorProductoId(productoId);
    }

    @PostMapping
    @Operation(summary = "Inicializar el inventario de un producto (idempotente)")
    @ApiResponse(responseCode = "201", description = "Inventario creado, con stock inicial en cero")
    @ApiResponse(responseCode = "200", description = "El inventario ya existía")
    public ResponseEntity<InventarioResponse> inicializar(@PathVariable Long productoId) {
        ResultadoInicializacion resultado = service.inicializarInventario(productoId);
        if (resultado.creado()) {
            URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
            return ResponseEntity.created(location).body(resultado.inventario());
        }
        return ResponseEntity.ok(resultado.inventario());
    }

    @PutMapping("/agregar")
    @Operation(summary = "Agregar stock (ingreso de mercadería)")
    @ApiResponse(responseCode = "200", description = "Stock actualizado")
    @ApiResponse(responseCode = "400", description = "Cantidad inválida")
    @ApiResponse(responseCode = "404", description = "El producto no tiene inventario")
    public InventarioResponse agregar(@PathVariable Long productoId,
            @Valid @RequestBody CantidadRequest request) {
        return service.agregarStock(productoId, request.cantidad());
    }

    @PutMapping("/reservar")
    @Operation(summary = "Reservar stock para una compra")
    @ApiResponse(responseCode = "200", description = "Stock reservado")
    @ApiResponse(responseCode = "400", description = "Cantidad inválida")
    @ApiResponse(responseCode = "404", description = "El producto no tiene inventario")
    @ApiResponse(responseCode = "409", description = "Stock insuficiente o conflicto de concurrencia")
    public InventarioResponse reservar(@PathVariable Long productoId,
            @Valid @RequestBody CantidadRequest request) {
        return service.reservarStock(productoId, request.cantidad());
    }

    @DeleteMapping
    @Operation(summary = "Eliminar el inventario de un producto")
    @ApiResponse(responseCode = "204", description = "Inventario eliminado exitosamente (o no existía)")
    public ResponseEntity<Void> eliminar(@PathVariable Long productoId) {
        service.eliminarInventario(productoId);
        return ResponseEntity.noContent().build();
    }
}