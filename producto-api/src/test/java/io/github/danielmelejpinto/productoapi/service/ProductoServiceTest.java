package io.github.danielmelejpinto.productoapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.danielmelejpinto.productoapi.dto.ProductoRequest;
import io.github.danielmelejpinto.productoapi.dto.ProductoResponse;
import io.github.danielmelejpinto.productoapi.exception.ProductoNoEncontradoException;
import io.github.danielmelejpinto.productoapi.model.Producto;
import io.github.danielmelejpinto.productoapi.repository.ProductoRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import io.github.danielmelejpinto.productoapi.exception.OrdenamientoInvalidoException;

import org.springframework.web.client.RestTemplate;

// MockitoExtension activa los mocks sin levantar Spring
@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository repository;

    @Mock
    private RestTemplate restTemplate; // cliente HTTP falso: no llama a inventario-api


    @InjectMocks
    private ProductoService service; // Mockito le pasa el mock al constructor

    // =============================================
    // Helpers
    // =============================================

    private Producto crearEntidad(Long id, String nombre, String precio) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setPrecio(new BigDecimal(precio));
        producto.setFechaCreacion(LocalDateTime.now());
        return producto;
    }

    private ProductoRequest crearRequest(String nombre, String precio) {
        ProductoRequest request = new ProductoRequest();
        request.setNombre(nombre);
        request.setPrecio(new BigDecimal(precio));
        return request;
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

        // Verificamos lo que devuelve el service
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNombre()).isEqualTo("Teclado");
        assertThat(response.getPrecio()).isEqualByComparingTo("50.00");
        assertThat(response.getFechaCreacion()).isNotNull();
    }

    // =============================================
    // obtenerTodos
    // =============================================

    @Test
    void obtenerTodos_conProductos_deberiaMapearLaPaginaAResponse() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(
                crearEntidad(1L, "Mouse", "25.00"),
                crearEntidad(2L, "Teclado", "50.00")), pageable, 2));

        Page<ProductoResponse> resultado = service.obtenerTodos(pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertThat(resultado.getContent()).hasSize(2);
        assertThat(resultado.getContent().get(0).getNombre()).isEqualTo("Mouse");
        assertThat(resultado.getContent().get(1).getNombre()).isEqualTo("Teclado");
    }

    @Test
    void obtenerTodos_sinProductos_deberiaDevolverPaginaVacia() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertThat(service.obtenerTodos(pageable).getContent()).isEmpty();
    }

    @Test
    void obtenerTodos_conCampoDeOrdenNoPermitido_deberiaLanzarExcepcionYNoConsultar() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("campoInventado"));

        assertThatThrownBy(() -> service.obtenerTodos(pageable))
                .isInstanceOf(OrdenamientoInvalidoException.class)
                .hasMessageContaining("campoInventado");
        verify(repository, never()).findAll(any(Pageable.class));
    }
    // =============================================
    // obtenerPorId
    // =============================================

    @Test
    void obtenerPorId_conIdExistente_deberiaDevolverElProducto() {
        when(repository.findById(1L)).thenReturn(Optional.of(crearEntidad(1L, "Monitor", "300.00")));

        ProductoResponse response = service.obtenerPorId(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNombre()).isEqualTo("Monitor");
    }

    @Test
    void obtenerPorId_conIdInexistente_deberiaLanzarProductoNoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorId(99L))
                .isInstanceOf(ProductoNoEncontradoException.class)
                // CAMBIO: el mensaje ahora incluye el id
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

        assertThat(response.getNombre()).isEqualTo("Auriculares Pro");
        assertThat(response.getPrecio()).isEqualByComparingTo("120.00");
        // JPA guarda los cambios al cerrar la transacción, por eso no se llama a save()
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
    void eliminar_conIdExistente_deberiaBorrarLaEntidad() {
        Producto existente = crearEntidad(1L, "Cable USB", "10.00");
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        service.eliminar(1L);

        verify(repository).delete(existente);
    }

    @Test
    void eliminar_conIdInexistente_deberiaLanzarExcepcionYNoBorrarNada() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(99L))
                .isInstanceOf(ProductoNoEncontradoException.class);
        verify(repository, never()).delete(any());
    }
}