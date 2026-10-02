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
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.SimpleTransactionStatus;

import io.github.danielmelejpinto.productoapi.model.EstadoEvento;
import io.github.danielmelejpinto.productoapi.model.EstadoProducto;
import io.github.danielmelejpinto.productoapi.model.OutboxEvent;
import io.github.danielmelejpinto.productoapi.model.Producto;
import io.github.danielmelejpinto.productoapi.model.TipoEvento;
import io.github.danielmelejpinto.productoapi.repository.OutboxEventRepository;
import io.github.danielmelejpinto.productoapi.repository.ProductoRepository;
import org.springframework.kafka.core.KafkaTemplate;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    @Mock
    private OutboxEventRepository eventRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private TransactionTemplate transactionTemplate;

    private OutboxProcessor processor;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            java.util.function.Consumer<SimpleTransactionStatus> action = invocation.getArgument(0);
            action.accept(new SimpleTransactionStatus());
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        org.mockito.Mockito.lenient().when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(null));

        processor = new OutboxProcessor(eventRepository, productoRepository, kafkaTemplate, transactionTemplate);
        org.springframework.test.util.ReflectionTestUtils.setField(processor, "maxIntentos", 5);
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

        when(eventRepository.findPendingEvents(any(), any())).thenReturn(List.of(event));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));

        processor.procesarEventosPendientes();

        verify(kafkaTemplate, never()).send(any(), any(), any());
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

        when(eventRepository.findPendingEvents(any(), any())).thenReturn(List.of(event));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));

        processor.procesarEventosPendientes();

        verify(kafkaTemplate).send("producto-events", "10", producto);
        assertThat(producto.getEstado()).isEqualTo(EstadoProducto.ACTIVO);
        assertThat(event.getEstado()).isEqualTo(EstadoEvento.ENVIADO);
        verify(productoRepository).save(producto);
        verify(eventRepository).save(event);
    }

    @Test
    void procesarEventosPendientes_kafkaRechazaElMensaje_noDeberiaMarcarEnviado() {
        OutboxEvent event = new OutboxEvent();
        event.setId(1L);
        event.setProductoId(10L);
        event.setEstado(EstadoEvento.PENDIENTE);
        event.setIntentos(0);

        Producto producto = new Producto();
        producto.setId(10L);
        producto.setEstado(EstadoProducto.PENDIENTE);

        when(eventRepository.findPendingEvents(any(), any())).thenReturn(List.of(event));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(kafkaTemplate.send(any(), any(), any()))
                .thenReturn(java.util.concurrent.CompletableFuture.failedFuture(new RuntimeException("broker rechazo")));

        processor.procesarEventosPendientes();

        assertThat(event.getEstado()).isEqualTo(EstadoEvento.PENDIENTE);
        assertThat(event.getIntentos()).isEqualTo(1);
        assertThat(producto.getEstado()).isEqualTo(EstadoProducto.PENDIENTE);
        verify(productoRepository, never()).save(producto);
    }

    @Test
    void procesarEventosPendientes_fallaTemporal_deberiaAumentarIntentosYNoCambiarEstado() {
        OutboxEvent event = new OutboxEvent();
        event.setId(1L);
        event.setProductoId(10L);
        event.setEstado(EstadoEvento.PENDIENTE);
        event.setIntentos(0); // Bajo el límite
        
        Producto producto = new Producto();
        producto.setId(10L);
        producto.setEstado(EstadoProducto.PENDIENTE);

        when(eventRepository.findPendingEvents(any(), any())).thenReturn(List.of(event));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        doThrow(new RuntimeException("Kafka down")).when(kafkaTemplate).send(any(), any(), any());

        processor.procesarEventosPendientes();

        assertThat(producto.getEstado()).isEqualTo(EstadoProducto.PENDIENTE);
        assertThat(event.getEstado()).isEqualTo(EstadoEvento.PENDIENTE);
        assertThat(event.getIntentos()).isEqualTo(1);
        verify(productoRepository, never()).save(producto);
        verify(eventRepository).save(event);
    }

    @Test
    void procesarEventosPendientes_fallaTemporalSuperaIntentos_deberiaMarcarError() {
        OutboxEvent event = new OutboxEvent();
        event.setId(1L);
        event.setProductoId(10L);
        event.setEstado(EstadoEvento.PENDIENTE);
        event.setIntentos(4); // Almacenará el intento 5 y llegará al límite de 5
        
        Producto producto = new Producto();
        producto.setId(10L);
        producto.setEstado(EstadoProducto.PENDIENTE);

        when(eventRepository.findPendingEvents(any(), any())).thenReturn(List.of(event));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        doThrow(new RuntimeException("Kafka down")).when(kafkaTemplate).send(any(), any(), any());

        processor.procesarEventosPendientes();

        assertThat(producto.getEstado()).isEqualTo(EstadoProducto.PENDIENTE);
        assertThat(event.getEstado()).isEqualTo(EstadoEvento.ERROR);
        assertThat(event.getIntentos()).isEqualTo(5);
        verify(productoRepository, never()).save(producto);
        verify(eventRepository).save(event);
    }
}
