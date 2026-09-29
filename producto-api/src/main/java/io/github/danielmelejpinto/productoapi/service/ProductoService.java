package io.github.danielmelejpinto.productoapi.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.danielmelejpinto.productoapi.dto.ProductoRequest;
import io.github.danielmelejpinto.productoapi.dto.ProductoResponse;
import io.github.danielmelejpinto.productoapi.exception.OrdenamientoInvalidoException;
import io.github.danielmelejpinto.productoapi.exception.ProductoNoEncontradoException;
import io.github.danielmelejpinto.productoapi.model.EstadoProducto;
import io.github.danielmelejpinto.productoapi.model.Producto;
import java.time.LocalDateTime;
import io.github.danielmelejpinto.productoapi.model.OutboxEvent;
import io.github.danielmelejpinto.productoapi.model.TipoEvento;
import io.github.danielmelejpinto.productoapi.model.EstadoEvento;
import io.github.danielmelejpinto.productoapi.repository.ProductoRepository;
import io.github.danielmelejpinto.productoapi.repository.OutboxEventRepository;

@Service
@Transactional(readOnly = true)
public class ProductoService {

    private static final String ID_FIELD = "id";


    private static final List<String> CAMPOS_ORDENABLES = List.of("id", "nombre", "precio", "fechaCreacion");

    private final ProductoRepository repository;
    private final OutboxEventRepository outboxEventRepository;

    public ProductoService(ProductoRepository repository, OutboxEventRepository outboxEventRepository) {
        this.repository = repository;
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {
        Producto producto = new Producto();
        producto.setNombre(request.nombre());
        producto.setPrecio(request.precio());
        producto.setEstado(EstadoProducto.PENDIENTE);

        Producto productoGuardado = repository.save(producto);

        OutboxEvent event = new OutboxEvent();
        event.setProductoId(productoGuardado.getId());
        event.setTipoEvento(TipoEvento.CREACION);
        event.setEstado(EstadoEvento.PENDIENTE);
        event.setFechaCreacion(LocalDateTime.now());
        event.setIntentos(0);
        outboxEventRepository.save(event);

        return mapearAResponse(productoGuardado);
    }

    public Page<ProductoResponse> obtenerTodos(Pageable pageable) {
        validarOrdenamiento(pageable.getSort());
        
        // Agregar "id" como desempate para paginación estable si no está presente como criterio único principal (siempre lo anexamos)
        Sort sort = pageable.getSort().and(Sort.by(ID_FIELD));
        Pageable pageableConId = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        
        return repository.findByEstado(EstadoProducto.ACTIVO, pageableConId).map(this::mapearAResponse);
    }

    public ProductoResponse obtenerPorId(Long id) {
        return mapearAResponse(buscarEntidadPorId(id));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {
        Producto producto = buscarEntidadPorId(id);
        producto.setNombre(request.nombre());
        producto.setPrecio(request.precio());
        // No hace falta repository.save(): la entidad está gestionada por JPA
        return mapearAResponse(producto);
    }

    @Transactional
    public void eliminar(Long id) {
        Producto producto = buscarEntidadPorId(id);
        producto.setEstado(EstadoProducto.BAJA);
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
        Producto producto = repository.findById(id)
                .orElseThrow(() -> new ProductoNoEncontradoException(id));
        if (producto.getEstado() == EstadoProducto.BAJA) {
            throw new ProductoNoEncontradoException(id);
        }
        return producto;
    }

    private ProductoResponse mapearAResponse(Producto producto) {
        return new ProductoResponse(
                producto.getId(),
                producto.getNombre(),
                producto.getPrecio(),
                producto.getFechaCreacion(),
                producto.getEstado());
    }
}