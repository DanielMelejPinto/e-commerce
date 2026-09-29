package io.github.danielmelejpinto.productoapi.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);

    private final OutboxEventRepository eventRepository;
    private final ProductoRepository productoRepository;
    private final InventarioClient inventarioClient;

    @Value("${outbox.max-intentos:5}")
    private int maxIntentos;

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
                log.error("Producto no existe para evento {}", event.getId());
                event.setEstado(EstadoEvento.ERROR);
                eventRepository.save(event);
                continue;
            }

            // Si el producto ya fue dado de baja, no llamamos a inventario y lo descartamos/marcamos enviado
            if (producto.getEstado() == EstadoProducto.BAJA) {
                log.info("Evento {} enviado correctamente para producto {}", event.getId(), producto.getId());
                event.setEstado(EstadoEvento.ENVIADO);
                eventRepository.save(event);
                continue;
            }

            try {
                inventarioClient.inicializarInventario(producto.getId());
                
                log.info("Evento {} enviado correctamente para producto {}", event.getId(), producto.getId());
                event.setEstado(EstadoEvento.ENVIADO);
                producto.setEstado(EstadoProducto.ACTIVO);
                
                productoRepository.save(producto);
                eventRepository.save(event);
            } catch (InventarioRechazoException e) {
                // 4xx: Error de contrato, permanente
                log.error("Rechazo permanente para evento {} del producto {}", event.getId(), producto.getId());
                event.setEstado(EstadoEvento.ERROR);
                producto.setEstado(EstadoProducto.BAJA);
                
                productoRepository.save(producto);
                eventRepository.save(event);
            } catch (Exception e) {
                // 5xx o timeout: Reintentar
                event.setIntentos(event.getIntentos() + 1);
                if (event.getIntentos() >= maxIntentos) {
                    log.error("Evento {} alcanzó el máximo de intentos ({}). Producto: {}. Marcando ERROR.", event.getId(), maxIntentos, producto.getId());
                    event.setEstado(EstadoEvento.ERROR);
                } else {
                    log.warn("Fallo recuperable en evento {} del producto {}. Intentos: {}. Error: {}", event.getId(), producto.getId(), event.getIntentos(), e.getMessage());
                }
                eventRepository.save(event);
            }
        }
    }
}
