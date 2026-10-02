package io.github.danielmelejpinto.productoapi.controller;

import io.github.danielmelejpinto.productoapi.dto.ProductoResponse;
import io.github.danielmelejpinto.productoapi.model.Producto;
import io.github.danielmelejpinto.productoapi.repository.ProductoRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/productos/historial")
public class HistorialController {

    private final ProductoRepository repository;

    public HistorialController(ProductoRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    @Operation(summary = "Obtener productos por lista de IDs, sin filtrar por estado. Para historial de compras.")
    public ResponseEntity<List<ProductoResponse>> obtenerPorIds(@RequestBody List<Long> ids) {
        List<Producto> productos = repository.findAllById(ids);
        List<ProductoResponse> responses = productos.stream().map(p -> new ProductoResponse(
                p.getId(),
                p.getNombre(),
                p.getPrecio(),
                p.getDescripcion(),
                p.getImagenUrl(),
                p.getFechaCreacion(),
                p.getEstado()
        )).collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }
}
