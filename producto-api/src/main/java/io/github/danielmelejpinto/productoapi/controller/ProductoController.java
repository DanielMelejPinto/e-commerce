package io.github.danielmelejpinto.productoapi.controller;

import java.net.URI;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
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

import io.github.danielmelejpinto.productoapi.dto.ProductoRequest;
import io.github.danielmelejpinto.productoapi.dto.ProductoResponse;
import io.github.danielmelejpinto.productoapi.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/productos")
@Tag(name = "Productos", description = "Operaciones CRUD sobre el catálogo de productos")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Crear un producto")
    @ApiResponse(responseCode = "201", description = "Producto creado (el header Location trae su URL)")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse nuevo = service.crear(request);
        // Header Location apuntando al recurso recién creado
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(nuevo.id())
                .toUri();
        return ResponseEntity.created(location).body(nuevo);
    }

    // Spring arma el Pageable desde ?page=0&size=10&sort=precio,desc
    @GetMapping
    @Operation(summary = "Listar productos con paginación y orden", description = "Parámetros: page, size (máx. 50), sort y nombre. Ej: ?nombre=Laptop&sort=precio,desc")
    @ApiResponse(responseCode = "200", description = "Página de productos")
    @ApiResponse(responseCode = "400", description = "Campo de orden no permitido")
    public ResponseEntity<PagedModel<ProductoResponse>> listarTodos(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String nombre,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(service.obtenerTodos(nombre, pageable)));
    }

    @GetMapping("/admin")
    @Operation(summary = "Listar todos los productos (incluidos pendientes/baja) para administradores")
    public ResponseEntity<PagedModel<ProductoResponse>> listarTodosAdmin(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String nombre,
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(new PagedModel<>(service.obtenerTodosAdmin(nombre, pageable)));
    }

    @PostMapping("/outbox/reintentar")
    @Operation(summary = "Reintentar manualmente eventos fallidos")
    public ResponseEntity<Void> reintentarEventosFallidos(
        @org.springframework.beans.factory.annotation.Autowired io.github.danielmelejpinto.productoapi.service.OutboxProcessor processor) {
        processor.reintentarEventosFallidos();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un producto por id")
    @ApiResponse(responseCode = "200", description = "Producto encontrado")
    @ApiResponse(responseCode = "400", description = "El id no es numérico")
    @ApiResponse(responseCode = "404", description = "El producto no existe")
    public ResponseEntity<ProductoResponse> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtenerPorId(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un producto")
    @ApiResponse(responseCode = "200", description = "Producto actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "404", description = "El producto no existe")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable Long id,
            @Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.ok(service.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un producto")
    @ApiResponse(responseCode = "204", description = "Producto eliminado")
    @ApiResponse(responseCode = "404", description = "El producto no existe")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}