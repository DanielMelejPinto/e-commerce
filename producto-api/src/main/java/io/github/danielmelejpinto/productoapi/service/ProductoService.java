package io.github.danielmelejpinto.productoapi.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.danielmelejpinto.productoapi.dto.ProductoRequest;
import io.github.danielmelejpinto.productoapi.dto.ProductoResponse;
import io.github.danielmelejpinto.productoapi.exception.ProductoNoEncontradoException;
import io.github.danielmelejpinto.productoapi.model.Producto;
import io.github.danielmelejpinto.productoapi.repository.ProductoRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository repository;

    public ProductoService(ProductoRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setPrecio(request.getPrecio());
        return mapearAResponse(repository.save(producto));
    }

    public List<ProductoResponse> obtenerTodos() {
        return repository.findAll()
                .stream()
                .map(this::mapearAResponse)
                .toList();
    }

    public ProductoResponse obtenerPorId(Long id) {
        return mapearAResponse(buscarEntidadPorId(id));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscarEntidadPorId(id);
        producto.setNombre(request.getNombre());
        producto.setPrecio(request.getPrecio());
        // No hace falta repository.save(): la entidad está gestionada por JPA
        // y los cambios se persisten al cerrar la transacción
        return mapearAResponse(producto);
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(buscarEntidadPorId(id));
    }

    // --- Métodos privados de apoyo ---

    private Producto buscarEntidadPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ProductoNoEncontradoException(id));
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