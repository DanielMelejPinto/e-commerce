package io.github.danielmelejpinto.productoapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import io.github.danielmelejpinto.productoapi.repository.OutboxEventRepository;
import io.github.danielmelejpinto.productoapi.dto.ProductoRequest;
import io.github.danielmelejpinto.productoapi.dto.ProductoResponse;
import io.github.danielmelejpinto.productoapi.exception.OrdenamientoInvalidoException;
import io.github.danielmelejpinto.productoapi.exception.ProductoNoEncontradoException;
import io.github.danielmelejpinto.productoapi.model.EstadoProducto;
import io.github.danielmelejpinto.productoapi.model.OutboxEvent;
import io.github.danielmelejpinto.productoapi.model.EstadoEvento;
import io.github.danielmelejpinto.productoapi.model.TipoEvento;
import io.github.danielmelejpinto.productoapi.model.Producto;
import io.github.danielmelejpinto.productoapi.repository.ProductoRepository;

// MockitoExtension activa los mocks sin levantar Spring
@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    private static final String ID_FIELD = "id";
    private static final String CAMPO_INVENTADO = "campoInventado";

    @Mock
    private ProductoRepository repository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    private ProductoService service;

    @BeforeEach
    void setUp() {
        service = new ProductoService(repository, outboxEventRepository);
    }

    // =============================================
    // Helpers
    // =============================================

    private Producto crearEntidad(Long id, String nombre, String precio, EstadoProducto estado) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setPrecio(new BigDecimal(precio));
        producto.setFechaCreacion(LocalDateTime.now());
        producto.setEstado(estado);
        return producto;
    }

    private Producto crearEntidad(Long id, String nombre, String precio) {
        return crearEntidad(id, nombre, precio, EstadoProducto.ACTIVO);
    }

    private ProductoRequest crearRequest(String nombre, String precio) {
        return new ProductoRequest(nombre, new BigDecimal(precio));
    }

    // =============================================
    // crear
    // =============================================

    @Test
    void crear_deberiaGuardarLaEntidadYDevolverElResponse() {
        // El mock simula lo que haría la BD: asignar un id al guardar
        when(repository.save(any(Producto.class))).thenAnswer(invocacion -> {
            Producto p = invocacion.getArgument(0);
            p.setId(1L);
            p.setFechaCreacion(LocalDateTime.now());
            return p;
        });

        ProductoResponse response = service.crear(crearRequest("Teclado", "50.00"));

        // Verificamos qué se le pasó al repositorio
        ArgumentCaptor<Producto> captor = ArgumentCaptor.forClass(Producto.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getNombre()).isEqualTo("Teclado");
        assertThat(captor.getValue().getPrecio()).isEqualByComparingTo("50.00");
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoProducto.PENDIENTE);

        // Verificamos lo que devuelve el service
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nombre()).isEqualTo("Teclado");
        assertThat(response.precio()).isEqualByComparingTo("50.00");
        assertThat(response.fechaCreacion()).isNotNull();
        assertThat(response.estado()).isEqualTo(EstadoProducto.PENDIENTE);
    }

    @Test
    void crear_deberiaGuardarEventoEnOutbox() {
        when(repository.save(any(Producto.class))).thenAnswer(invocacion -> {
            Producto p = invocacion.getArgument(0);
            p.setId(7L);
            p.setFechaCreacion(LocalDateTime.now());
            return p;
        });

        service.crear(crearRequest("Teclado", "50.00"));

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());
        assertThat(captor.getValue().getProductoId()).isEqualTo(7L);
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoEvento.PENDIENTE);
        assertThat(captor.getValue().getTipoEvento()).isEqualTo(TipoEvento.CREACION);
    }

    // =============================================
    // obtenerTodos
    // =============================================

    @Test
    void obtenerTodos_conProductos_deberiaMapearLaPaginaAResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        Sort sortId = pageable.getSort().and(Sort.by(ID_FIELD));
        Pageable expectedPageable = PageRequest.of(0, 10, sortId);

        when(repository.findByEstado(eq(EstadoProducto.ACTIVO), eq(expectedPageable))).thenReturn(new PageImpl<>(List.of(
                crearEntidad(1L, "Mouse", "25.00"),
                crearEntidad(2L, "Teclado", "50.00")), expectedPageable, 2));

        Page<ProductoResponse> resultado = service.obtenerTodos(pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getContent().get(0).nombre()).isEqualTo("Mouse");
        assertThat(resultado.getContent().get(1).nombre()).isEqualTo("Teclado");
    }

    @Test
    void obtenerTodos_sinProductos_deberiaDevolverPaginaVacia() {
        Pageable pageable = PageRequest.of(0, 10);
        Sort sortId = pageable.getSort().and(Sort.by(ID_FIELD));
        Pageable expectedPageable = PageRequest.of(0, 10, sortId);

        when(repository.findByEstado(eq(EstadoProducto.ACTIVO), eq(expectedPageable))).thenReturn(new PageImpl<>(List.of(), expectedPageable, 0));

        assertThat(service.obtenerTodos(pageable).getContent()).isEmpty();
    }

    @Test
    void obtenerTodos_conCampoDeOrdenNoPermitido_deberiaLanzarExcepcionYNoConsultar() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(CAMPO_INVENTADO));

        assertThatThrownBy(() -> service.obtenerTodos(pageable))
                .isInstanceOf(OrdenamientoInvalidoException.class)
                .hasMessageContaining("campoInventado");
        verify(repository, never()).findByEstado(any(), any());
    }

    // =============================================
    // obtenerPorId
    // =============================================

    @Test
    void obtenerPorId_conIdExistenteYActivo_deberiaDevolverElProducto() {
        when(repository.findById(1L)).thenReturn(Optional.of(crearEntidad(1L, "Monitor", "300.00")));

        ProductoResponse response = service.obtenerPorId(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.nombre()).isEqualTo("Monitor");
    }

    @Test
    void obtenerPorId_conIdExistenteYDeBaja_deberiaLanzarNoEncontrado() {
        when(repository.findById(1L)).thenReturn(Optional.of(crearEntidad(1L, "Monitor", "300.00", EstadoProducto.BAJA)));

        assertThatThrownBy(() -> service.obtenerPorId(1L))
                .isInstanceOf(ProductoNoEncontradoException.class)
                .hasMessage("Producto con id 1 no existe");
    }

    @Test
    void obtenerPorId_conIdInexistente_deberiaLanzarProductoNoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(99L))
                .isInstanceOf(ProductoNoEncontradoException.class)
                .hasMessage("Producto con id 99 no existe");
    }

    // =============================================
    // actualizar
    // =============================================

    @Test
    void actualizar_conIdExistente_deberiaModificarLosCamposDeLaEntidad() {
        Producto existente = crearEntidad(1L, "Auriculares", "80.00");
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        ProductoResponse response = service.actualizar(1L, crearRequest("Auriculares Pro", "120.00"));

        assertThat(response.nombre()).isEqualTo("Auriculares Pro");
        assertThat(response.precio()).isEqualByComparingTo("120.00");
        verify(repository, never()).save(any());
    }

    @Test
    void actualizar_conIdBaja_deberiaLanzarNoEncontrado() {
        Producto existente = crearEntidad(1L, "Auriculares", "80.00", EstadoProducto.BAJA);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.actualizar(1L, crearRequest("Auriculares Pro", "120.00")))
                .isInstanceOf(ProductoNoEncontradoException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void actualizar_conIdInexistente_deberiaLanzarProductoNoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.actualizar(99L, crearRequest("Producto", "50.00")))
                .isInstanceOf(ProductoNoEncontradoException.class);
        verify(repository, never()).save(any());
    }

    // =============================================
    // eliminar
    // =============================================

    @Test
    void eliminar_conIdExistente_deberiaCambiarEstadoABajaYNoLlamarInventario() {
        Producto existente = crearEntidad(1L, "Cable USB", "10.00");
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        service.eliminar(1L);

        assertThat(existente.getEstado()).isEqualTo(EstadoProducto.BAJA);
        verify(repository, never()).delete(any());

    }

    @Test
    void eliminar_conIdBaja_deberiaLanzarNoEncontrado() {
        Producto existente = crearEntidad(1L, "Cable USB", "10.00", EstadoProducto.BAJA);
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.eliminar(1L))
                .isInstanceOf(ProductoNoEncontradoException.class);

        verify(repository, never()).delete(any());

    }

    @Test
    void eliminar_conIdInexistente_deberiaLanzarExcepcionYNoBorrarNada() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(99L))
                .isInstanceOf(ProductoNoEncontradoException.class);
        verify(repository, never()).delete(any());

    }
}
