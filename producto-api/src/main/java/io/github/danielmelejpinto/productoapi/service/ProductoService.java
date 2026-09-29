package io.github.danielmelejpinto.productoapi.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate; // NUEVO: Import de RestTemplate

import io.github.danielmelejpinto.productoapi.dto.ProductoRequest;
import io.github.danielmelejpinto.productoapi.dto.ProductoResponse;
import io.github.danielmelejpinto.productoapi.exception.OrdenamientoInvalidoException;
import io.github.danielmelejpinto.productoapi.exception.ProductoNoEncontradoException;
import io.github.danielmelejpinto.productoapi.model.Producto;
import io.github.danielmelejpinto.productoapi.repository.ProductoRepository;

@Service
@Transactional(readOnly = true)
public class ProductoService {

    private static final List<String> CAMPOS_ORDENABLES = List.of("id", "nombre", "precio", "fechaCreacion");

    private final ProductoRepository repository;
    private final RestTemplate restTemplate; // NUEVO: Variable declarada

    // NUEVO: Constructor actualizado para inyectar RestTemplate
    public ProductoService(ProductoRepository repository, RestTemplate restTemplate) {
        this.repository = repository;
        this.restTemplate = restTemplate;
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Producto producto = new Producto();
        producto.setNombre(request.getNombre());
        producto.setPrecio(request.getPrecio());
        
        // NUEVO: Guardamos el producto y capturamos la entidad guardada
        Producto productoGuardado = repository.save(producto);
        
        // NUEVO: Hacemos la llamada al otro microservicio
        String url = "http://localhost:8081/api/inventarios/producto/" + productoGuardado.getId();
        restTemplate.postForObject(url, null, Void.class);
        
        return mapearAResponse(productoGuardado);
    }

    public Page<ProductoResponse> obtenerTodos(Pageable pageable) {
        validarOrdenamiento(pageable.getSort());
        return repository.findAll(pageable).map(this::mapearAResponse);
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
        return mapearAResponse(producto);
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(buscarEntidadPorId(id));
    }

    // --- Métodos privados de apoyo ---

    private void validarOrdenamiento(Sort sort) {
        for (Sort.Order orden : sort) {
            if (!CAMPOS_ORDENABLES.contains(orden.getProperty())) {
                throw new OrdenamientoInvalidoException(orden.getProperty(), CAMPOS_ORDENABLES);
            }
        }
    }

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