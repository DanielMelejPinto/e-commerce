package io.github.danielmelejpinto.inventarioapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.danielmelejpinto.inventarioapi.dto.InventarioResponse;
import io.github.danielmelejpinto.inventarioapi.dto.ResultadoInicializacion;
import io.github.danielmelejpinto.inventarioapi.exception.InventarioNoEncontradoException;
import io.github.danielmelejpinto.inventarioapi.exception.StockInsuficienteException;
import io.github.danielmelejpinto.inventarioapi.model.Inventario;
import io.github.danielmelejpinto.inventarioapi.repository.InventarioRepository;
import io.github.danielmelejpinto.inventarioapi.repository.ReservaRepository;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {
    @Test
    void inicializarInventario_cuandoColisionaConConcurrencia_deberiaDevolverExistenteYNoPropagarExcepcion() {
        Long productoId = 99L;
        Inventario existente = new Inventario();
        existente.setProductoId(productoId);
        existente.setCantidadDisponible(0L);
        existente.setCantidadReservada(0L);

        // La primera vez devuelve vacío, intentamos guardar y da error de integridad,
        // luego vuelve a buscar y encuentra el que insertó el otro hilo.
        when(repository.findByProductoId(productoId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existente));
                
        when(repository.save(any(Inventario.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Colisión simulada"));

        ResultadoInicializacion resultado = service.inicializarInventario(productoId);

        assertThat(resultado.inventario().productoId()).isEqualTo(existente.getProductoId());
        assertThat(resultado.creado()).isFalse();
        
        // Verificamos que guardó
        verify(repository).save(any(Inventario.class));
        // Verificamos que buscó 2 veces
        verify(repository, org.mockito.Mockito.times(2)).findByProductoId(productoId);
    }


    @Mock
    private InventarioRepository repository;

    @Mock
    private ReservaRepository reservaRepository;

    @InjectMocks
    private InventarioService service;

    @Test
    void obtenerPorProductoId_conIdExistente_deberiaRetornarInventario() {
        Long productoId = 1L;
        Inventario inventario = new Inventario();
        inventario.setProductoId(productoId);
        inventario.setCantidadDisponible(10L);
        inventario.setCantidadReservada(5L);
        inventario.setUltimaActualizacion(LocalDateTime.now());

        when(repository.findByProductoId(productoId)).thenReturn(Optional.of(inventario));

        InventarioResponse response = service.obtenerPorProductoId(productoId);

        assertThat(response.productoId()).isEqualTo(productoId);
        assertThat(response.cantidadDisponible()).isEqualTo(10L);
        assertThat(response.cantidadReservada()).isEqualTo(5L);
    }

    @Test
    void obtenerPorProductoId_conIdInexistente_deberiaLanzarExcepcion() {
        Long productoId = 1L;
        when(repository.findByProductoId(productoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerPorProductoId(productoId))
                .isInstanceOf(InventarioNoEncontradoException.class)
                .hasMessageContaining("No existe inventario para el producto con id 1");
    }

    @Test
    void inicializarInventario_cuandoNoExiste_deberiaCrearConStockCero() {
        Long productoId = 1L;
        when(repository.findByProductoId(productoId)).thenReturn(Optional.empty());
        when(repository.save(any(Inventario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResultadoInicializacion resultado = service.inicializarInventario(productoId);
        InventarioResponse response = resultado.inventario();

        assertThat(resultado.creado()).isTrue();
        assertThat(response.productoId()).isEqualTo(productoId);
        assertThat(response.cantidadDisponible()).isZero();
        assertThat(response.cantidadReservada()).isZero();
        verify(repository).save(any(Inventario.class));
    }

    @Test
    void inicializarInventario_cuandoYaExiste_deberiaRetornarExistenteYNoGuardarDeNuevo() {
        Long productoId = 1L;
        Inventario inventario = new Inventario();
        inventario.setProductoId(productoId);
        inventario.setCantidadDisponible(10L);
        inventario.setCantidadReservada(5L);

        when(repository.findByProductoId(productoId)).thenReturn(Optional.of(inventario));

        ResultadoInicializacion resultado = service.inicializarInventario(productoId);
        InventarioResponse response = resultado.inventario();

        assertThat(resultado.creado()).isFalse();
        assertThat(response.cantidadDisponible()).isEqualTo(10L);
        verify(repository, never()).save(any(Inventario.class));
    }

    @Test
    void inicializarInventario_conColision_deberiaRecuperarYRetornarExistente() {
        Long productoId = 1L;
        Inventario inventarioExistente = new Inventario();
        inventarioExistente.setProductoId(productoId);
        inventarioExistente.setCantidadDisponible(20L);

        when(repository.findByProductoId(productoId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(inventarioExistente));

        when(repository.save(any(Inventario.class)))
                .thenThrow(new DataIntegrityViolationException("Colisión"));

        ResultadoInicializacion resultado = service.inicializarInventario(productoId);

        assertThat(resultado.creado()).isFalse();
        assertThat(resultado.inventario().cantidadDisponible()).isEqualTo(20L);
        verify(repository).save(any(Inventario.class));
    }

    @Test
    void agregarStock_conIdExistente_deberiaSumarAlStockDisponible() {
        Long productoId = 1L;
        Inventario inventario = new Inventario();
        inventario.setProductoId(productoId);
        inventario.setCantidadDisponible(10L);
        inventario.setCantidadReservada(0L);

        when(repository.findByProductoId(productoId)).thenReturn(Optional.of(inventario));

        InventarioResponse response = service.agregarStock(productoId, 5);

        assertThat(response.cantidadDisponible()).isEqualTo(15L);
    }

    @Test
    void agregarStock_conIdInexistente_deberiaLanzarExcepcion() {
        Long productoId = 1L;
        when(repository.findByProductoId(productoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.agregarStock(productoId, 5))
                .isInstanceOf(InventarioNoEncontradoException.class);
    }

    @Test
    void reservarStock_conStockSuficiente_deberiaMoverDisponibleAReservada() {
        Long productoId = 1L;
        Inventario inventario = new Inventario();
        inventario.setProductoId(productoId);
        inventario.setCantidadDisponible(5L);
        inventario.setCantidadReservada(5L);

        when(repository.reservarStockAtomico(productoId, 5)).thenReturn(1);
        when(repository.findByProductoId(productoId)).thenReturn(Optional.of(inventario));

        InventarioResponse response = service.reservarStock(productoId, 5, 99L);

        assertThat(response.cantidadDisponible()).isEqualTo(5L);
        assertThat(response.cantidadReservada()).isEqualTo(5L);
    }

    @Test
    void reservarStock_conStockExacto_deberiaPermitirReserva() {
        Long productoId = 1L;
        Inventario inventario = new Inventario();
        inventario.setProductoId(productoId);
        inventario.setCantidadDisponible(0L);
        inventario.setCantidadReservada(10L);

        when(repository.reservarStockAtomico(productoId, 10)).thenReturn(1);
        when(repository.findByProductoId(productoId)).thenReturn(Optional.of(inventario));

        InventarioResponse response = service.reservarStock(productoId, 10, 99L);

        assertThat(response.cantidadDisponible()).isZero();
        assertThat(response.cantidadReservada()).isEqualTo(10L);
    }

    @Test
    void reservarStock_conStockInsuficiente_deberiaLanzarExcepcionYNoModificarStock() {
        Long productoId = 1L;
        Inventario inventario = new Inventario();
        inventario.setProductoId(productoId);
        inventario.setCantidadDisponible(5L);
        inventario.setCantidadReservada(0L);

        when(repository.reservarStockAtomico(productoId, 10)).thenReturn(0);
        when(repository.findByProductoId(productoId)).thenReturn(Optional.of(inventario));

        assertThatThrownBy(() -> service.reservarStock(productoId, 10, 99L))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("disponible 5, solicitado 10");
    }

    @Test
    void reservarStock_conIdInexistente_deberiaLanzarExcepcion() {
        Long productoId = 1L;
        when(repository.reservarStockAtomico(productoId, 5)).thenReturn(0);
        when(repository.findByProductoId(productoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reservarStock(productoId, 5, 99L))
                .isInstanceOf(InventarioNoEncontradoException.class);
    }

    @Test
    void eliminarInventario_cuandoExiste_deberiaBorrarlo() {
        Long productoId = 1L;
        Inventario inventario = new Inventario();
        inventario.setProductoId(productoId);
        inventario.setCantidadReservada(0L);
        when(repository.findByProductoId(productoId)).thenReturn(Optional.of(inventario));

        service.eliminarInventario(productoId);

        verify(repository).delete(inventario);
    }

    @Test
    void eliminarInventario_cuandoNoExiste_noDeberiaHacerNada() {
        Long productoId = 1L;
        when(repository.findByProductoId(productoId)).thenReturn(Optional.empty());

        service.eliminarInventario(productoId);

        verify(repository, never()).delete(any(Inventario.class));
    }
}
