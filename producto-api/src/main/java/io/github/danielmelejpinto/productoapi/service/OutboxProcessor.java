package io.github.danielmelejpinto.productoapi.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate transactionTemplate;

    @Value("${outbox.max-intentos:5}")
    private int maxIntentos;

    public OutboxProcessor(OutboxEventRepository eventRepository, 
                           ProductoRepository productoRepository, 
                           InventarioClient inventarioClient,
                           TransactionTemplate transactionTemplate) {
        this.eventRepository = eventRepository;
        this.productoRepository = productoRepository;
        this.inventarioClient = inventarioClient;
        this.transactionTemplate = transactionTemplate;
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

            if (producto.getEstado() == EstadoProducto.BAJA) {
                event.setEstado(EstadoEvento.ENVIADO);
                eventRepository.save(event);
                continue;
            }

            try {
                // 1. LLAMADA HTTP (Fuera de cualquier transacción de BD)
                inventarioClient.inicializarInventario(producto.getId());
                
                // 2. ACTUALIZACIÓN ATÓMICA DE BD LOCAL
                transactionTemplate.executeWithoutResult(status -> {
                    event.setEstado(EstadoEvento.ENVIADO);
                    producto.setEstado(EstadoProducto.ACTIVO);
                    
                    productoRepository.save(producto);
                    eventRepository.save(event);
                });
                
                log.info("Evento {} enviado correctamente para producto {}", event.getId(), producto.getId());
                
            } catch (InventarioRechazoException e) {
                // Rechazo permanente por reglas de negocio (ej. 400 Bad Request)
                log.error("Rechazo permanente para evento {} del producto {}", event.getId(), producto.getId());
                transactionTemplate.executeWithoutResult(status -> {
                    event.setEstado(EstadoEvento.ERROR);
                    producto.setEstado(EstadoProducto.BAJA);
                    productoRepository.save(producto);
                    eventRepository.save(event);
                });
                
            } catch (Exception e) {
                // Si llegamos aquí, o la llamada HTTP falló, o la transacción de BD falló.
                // En ambos casos, el evento no se pudo completar.
                
                // Forzamos que retorne a PENDIENTE (por si falló en memoria pero no en BD)
                event.setEstado(EstadoEvento.PENDIENTE);
                event.setIntentos(event.getIntentos() + 1);
                
                if (event.getIntentos() >= maxIntentos) {
                    log.error("Evento {} superó intentos. Marcando ERROR. Error: {}", event.getId(), e.getMessage());
                    event.setEstado(EstadoEvento.ERROR);
                } else {
                    log.warn("Fallo temporal en evento {}. Intentos: {}. Error: {}", event.getId(), event.getIntentos(), e.getMessage());
                }
                eventRepository.save(event); // Guardamos la actualización de intentos
            }
        }
    }
}