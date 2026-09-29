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
import io.github.danielmelejpinto.inventarioapi.exception.InventarioNoEncontradoException;
import io.github.danielmelejpinto.inventarioapi.exception.StockInsuficienteException;
import io.github.danielmelejpinto.inventarioapi.model.Inventario;
import io.github.danielmelejpinto.inventarioapi.repository.InventarioRepository;

@ExtendWith(MockitoExtension.class)
class InventarioServiceTest {

    @Mock
    private InventarioRepository repository;

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

        InventarioResponse response = service.inicializarInventario(productoId);

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

        InventarioResponse response = service.inicializarInventario(productoId);

        assertThat(response.cantidadDisponible()).isEqualTo(10L);
        verify(repository, never()).save(any(Inventario.class));
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
        inventario.setCantidadDisponible(10L);
        inventario.setCantidadReservada(0L);

        when(repository.findByProductoId(productoId)).thenReturn(Optional.of(inventario));

        InventarioResponse response = service.reservarStock(productoId, 5);

        assertThat(response.cantidadDisponible()).isEqualTo(5L);
        assertThat(response.cantidadReservada()).isEqualTo(5L);
    }
    
    @Test
    void reservarStock_conStockExacto_deberiaPermitirReserva() {
        Long productoId = 1L;
        Inventario inventario = new Inventario();
        inventario.setProductoId(productoId);
        inventario.setCantidadDisponible(10L);
        inventario.setCantidadReservada(0L);

        when(repository.findByProductoId(productoId)).thenReturn(Optional.of(inventario));

        InventarioResponse response = service.reservarStock(productoId, 10);

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

        when(repository.findByProductoId(productoId)).thenReturn(Optional.of(inventario));

        assertThatThrownBy(() -> service.reservarStock(productoId, 10))
                .isInstanceOf(StockInsuficienteException.class)
                .hasMessageContaining("disponible 5, solicitado 10");

        assertThat(inventario.getCantidadDisponible()).isEqualTo(5L);
    }
    
    @Test
    void reservarStock_conIdInexistente_deberiaLanzarExcepcion() {
        Long productoId = 1L;
        when(repository.findByProductoId(productoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reservarStock(productoId, 5))
                .isInstanceOf(InventarioNoEncontradoException.class);
    }
    
    @Test
    void eliminarInventario_cuandoExiste_deberiaBorrarlo() {
        Long productoId = 1L;
        Inventario inventario = new Inventario();
        inventario.setProductoId(productoId);
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
