package io.github.danielmelejpinto.productoapi.service;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import io.github.danielmelejpinto.productoapi.model.Producto;
import io.github.danielmelejpinto.productoapi.repository.ProductoRepository;
import io.github.danielmelejpinto.productoapi.dto.ProductoRequest;
import io.github.danielmelejpinto.productoapi.dto.ProductoResponse;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductoService {

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    public ProductoResponse crearProducto(ProductoRequest request) {
        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setPrecio(request.getPrecio());
        
        Producto guardado = repository.save(producto);
        return mapearAResponse(guardado);
    }

    public List<ProductoResponse> obtenerTodos() {
        return repository.findAll()
                .stream()
                .map(this::mapearAResponse)
                .collect(Collectors.toList());
    }

    public ProductoResponse obtenerPorId(Long id) {
        Producto producto = buscarEntidadPorId(id);
        return mapearAResponse(producto);
    }

    public ProductoResponse actualizarProducto(Long id, ProductoRequest request) {
        Producto productoExistente = buscarEntidadPorId(id);
        
        productoExistente.setNombre(request.getNombre());
        productoExistente.setPrecio(request.getPrecio());
        
        Producto actualizado = repository.save(productoExistente); 
        return mapearAResponse(actualizado);
    }

    public void eliminarProducto(Long id) {
        Producto productoExistente = buscarEntidadPorId(id);
        repository.delete(productoExistente);
    }

    private Producto buscarEntidadPorId(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El producto no existe"));
    }

    private ProductoResponse mapearAResponse(Producto producto) {
        ProductoResponse response = new ProductoResponse();
        response.setId(producto.getId());
        response.setNombre(producto.getNombre());
        response.setPrecio(producto.getPrecio());
        response.setFechaCreacion(producto.getFechaCreacion());
        return response;
    }
}