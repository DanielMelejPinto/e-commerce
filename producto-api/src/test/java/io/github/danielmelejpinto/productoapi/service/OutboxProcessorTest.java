package io.github.danielmelejpinto.productoapi.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.danielmelejpinto.productoapi.client.InventarioClient;
import io.github.danielmelejpinto.productoapi.exception.InventarioNoDisponibleException;
import io.github.danielmelejpinto.productoapi.exception.InventarioRechazoException;
import io.github.danielmelejpinto.productoapi.model.EstadoEvento;
import io.github.danielmelejpinto.productoapi.model.EstadoProducto;
import io.github.danielmelejpinto.productoapi.model.OutboxEvent;
import io.github.danielmelejpinto.productoapi.model.Producto;
import io.github.danielmelejpinto.productoapi.model.TipoEvento;
import io.github.danielmelejpinto.productoapi.repository.OutboxEventRepository;
import io.github.danielmelejpinto.productoapi.repository.ProductoRepository;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock
    private OutboxEventRepository eventRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private InventarioClient inventarioClient;

    private OutboxProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new OutboxProcessor(eventRepository, productoRepository, inventarioClient);
    }

    @Test
    void procesarEventosPendientes_conProductoEnBaja_noDebeLlamarAInventarioNiCambiarEstado() {
        OutboxEvent event = new OutboxEvent();
        event.setId(1L);
        event.setProductoId(10L);
        event.setEstado(EstadoEvento.PENDIENTE);
        event.setTipoEvento(TipoEvento.CREACION);

        Producto producto = new Producto();
        producto.setId(10L);
        producto.setEstado(EstadoProducto.BAJA);

        when(eventRepository.findByEstado(EstadoEvento.PENDIENTE)).thenReturn(List.of(event));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));

        processor.procesarEventosPendientes();

        verify(inventarioClient, never()).inicializarInventario(any());
        verify(productoRepository, never()).save(producto);
        
        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(eventRepository).save(captor.capture());
        
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoEvento.ENVIADO);
        assertThat(producto.getEstado()).isEqualTo(EstadoProducto.BAJA);
    }

    @Test
    void procesarEventosPendientes_exito_deberiaActivarYMarcarEnviado() {
        OutboxEvent event = new OutboxEvent();
        event.setId(1L);
        event.setProductoId(10L);
        event.setEstado(EstadoEvento.PENDIENTE);
        
        Producto producto = new Producto();
        producto.setId(10L);
        producto.setEstado(EstadoProducto.PENDIENTE);

        when(eventRepository.findByEstado(EstadoEvento.PENDIENTE)).thenReturn(List.of(event));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));

        processor.procesarEventosPendientes();

        verify(inventarioClient).inicializarInventario(10L);
        assertThat(producto.getEstado()).isEqualTo(EstadoProducto.ACTIVO);
        assertThat(event.getEstado()).isEqualTo(EstadoEvento.ENVIADO);
        verify(productoRepository).save(producto);
        verify(eventRepository).save(event);
    }

    @Test
    void procesarEventosPendientes_fallaPermanente_deberiaDarBajaYMarcarError() {
        OutboxEvent event = new OutboxEvent();
        event.setId(1L);
        event.setProductoId(10L);
        event.setEstado(EstadoEvento.PENDIENTE);
        
        Producto producto = new Producto();
        producto.setId(10L);
        producto.setEstado(EstadoProducto.PENDIENTE);

        when(eventRepository.findByEstado(EstadoEvento.PENDIENTE)).thenReturn(List.of(event));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        doThrow(new InventarioRechazoException("4xx")).when(inventarioClient).inicializarInventario(10L);

        processor.procesarEventosPendientes();

        assertThat(producto.getEstado()).isEqualTo(EstadoProducto.BAJA);
        assertThat(event.getEstado()).isEqualTo(EstadoEvento.ERROR);
        verify(productoRepository).save(producto);
        verify(eventRepository).save(event);
    }

    @Test
    void procesarEventosPendientes_fallaTemporal_deberiaAumentarIntentosYNoCambiarEstado() {
        OutboxEvent event = new OutboxEvent();
        event.setId(1L);
        event.setProductoId(10L);
        event.setEstado(EstadoEvento.PENDIENTE);
        event.setIntentos(0);
        
        Producto producto = new Producto();
        producto.setId(10L);
        producto.setEstado(EstadoProducto.PENDIENTE);

        when(eventRepository.findByEstado(EstadoEvento.PENDIENTE)).thenReturn(List.of(event));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        doThrow(new InventarioNoDisponibleException("5xx", null)).when(inventarioClient).inicializarInventario(10L);

        processor.procesarEventosPendientes();

        assertThat(producto.getEstado()).isEqualTo(EstadoProducto.PENDIENTE);
        assertThat(event.getEstado()).isEqualTo(EstadoEvento.PENDIENTE);
        assertThat(event.getIntentos()).isEqualTo(1);
        verify(productoRepository, never()).save(producto);
        verify(eventRepository).save(event);
    }
}
