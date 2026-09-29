package io.github.danielmelejpinto.productoapi.service;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.github.danielmelejpinto.productoapi.client.InventarioClient;
import io.github.danielmelejpinto.productoapi.exception.InventarioRechazoException;
import io.github.danielmelejpinto.productoapi.model.EstadoEvento;
import io.github.danielmelejpinto.productoapi.model.EstadoProducto;
import io.github.danielmelejpinto.productoapi.model.OutboxEvent;
import io.github.danielmelejpinto.productoapi.model.Producto;
import io.github.danielmelejpinto.productoapi.repository.OutboxEventRepository;
import io.github.danielmelejpinto.productoapi.repository.ProductoRepository;

@Component
public class OutboxProcessor {

    private final OutboxEventRepository eventRepository;
    private final ProductoRepository productoRepository;
    private final InventarioClient inventarioClient;

    public OutboxProcessor(OutboxEventRepository eventRepository, ProductoRepository productoRepository, InventarioClient inventarioClient) {
        this.eventRepository = eventRepository;
        this.productoRepository = productoRepository;
        this.inventarioClient = inventarioClient;
    }

    @Scheduled(fixedDelay = 5000)
    public void procesarEventosPendientes() {
        List<OutboxEvent> pendientes = eventRepository.findByEstado(EstadoEvento.PENDIENTE);
        
        for (OutboxEvent event : pendientes) {
            Producto producto = productoRepository.findById(event.getProductoId()).orElse(null);
            
            if (producto == null) {
                event.setEstado(EstadoEvento.ERROR);
                eventRepository.save(event);
                continue;
            }

            // Si el producto ya fue dado de baja, no llamamos a inventario y lo descartamos/marcamos enviado
            if (producto.getEstado() == EstadoProducto.BAJA) {
                event.setEstado(EstadoEvento.ENVIADO);
                eventRepository.save(event);
                continue;
            }

            try {
                inventarioClient.inicializarInventario(producto.getId());
                
                event.setEstado(EstadoEvento.ENVIADO);
                producto.setEstado(EstadoProducto.ACTIVO);
                
                productoRepository.save(producto);
                eventRepository.save(event);
            } catch (InventarioRechazoException e) {
                // 4xx: Error de contrato, permanente
                event.setEstado(EstadoEvento.ERROR);
                producto.setEstado(EstadoProducto.BAJA);
                
                productoRepository.save(producto);
                eventRepository.save(event);
            } catch (Exception e) {
                // 5xx o timeout: Reintentar
                event.setIntentos(event.getIntentos() + 1);
                eventRepository.save(event);
            }
        }
    }
}
