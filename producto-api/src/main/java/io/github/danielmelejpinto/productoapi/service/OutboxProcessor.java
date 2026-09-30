package io.github.danielmelejpinto.productoapi.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;



import io.github.danielmelejpinto.productoapi.model.EstadoEvento;
import io.github.danielmelejpinto.productoapi.model.EstadoProducto;
import io.github.danielmelejpinto.productoapi.model.OutboxEvent;
import io.github.danielmelejpinto.productoapi.model.Producto;
import io.github.danielmelejpinto.productoapi.repository.OutboxEventRepository;
import io.github.danielmelejpinto.productoapi.repository.ProductoRepository;

import org.springframework.kafka.core.KafkaTemplate;

@Component
public class OutboxProcessor {

    private static final Logger log = LoggerFactory.getLogger(OutboxProcessor.class);

    private final OutboxEventRepository eventRepository;
    private final ProductoRepository productoRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final TransactionTemplate transactionTemplate;

    @Value("${outbox.max-intentos:5}")
    private int maxIntentos;

    public OutboxProcessor(OutboxEventRepository eventRepository, 
                           ProductoRepository productoRepository, 
                           KafkaTemplate<String, Object> kafkaTemplate,
                           TransactionTemplate transactionTemplate) {
        this.eventRepository = eventRepository;
        this.productoRepository = productoRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Scheduled(fixedDelay = 5000)
    @SchedulerLock(name = "procesarEventosPendientesTask", lockAtLeastFor = "${outbox.lock-min:PT4S}", lockAtMostFor = "${outbox.lock-max:PT14M}")
    public void procesarEventosPendientes() {
        List<OutboxEvent> pendientes = eventRepository.findPendingEvents(EstadoEvento.PENDIENTE, LocalDateTime.now());
        
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
                // 1. PUBLICAR EN KAFKA (Fuera de la transacción de BD)
                kafkaTemplate.send("producto-events", String.valueOf(producto.getId()), producto);
                
                // 2. ACTUALIZACIÓN ATÓMICA DE BD LOCAL
                transactionTemplate.executeWithoutResult(status -> {
                    event.setEstado(EstadoEvento.ENVIADO);
                    producto.setEstado(EstadoProducto.ACTIVO);
                    
                    productoRepository.save(producto);
                    eventRepository.save(event);
                });
                
                log.info("Evento {} publicado en Kafka para producto {}", event.getId(), producto.getId());
                
            } catch (Exception e) {
                // Si llegamos aquí, o la llamada HTTP falló, o la transacción de BD falló.
                // En ambos casos, el evento no se pudo completar.
                
                event.setEstado(EstadoEvento.PENDIENTE);
                event.setIntentos(event.getIntentos() + 1);
                
                if (event.getIntentos() >= maxIntentos) {
                    log.error("Evento {} superó intentos. Marcando ERROR. Error: {}", event.getId(), e.getMessage());
                    event.setEstado(EstadoEvento.ERROR);
                } else {
                    // Exponential backoff: 2^intentos seconds (2s, 4s, 8s, 16s...)
                    long waitSeconds = (long) Math.pow(2, event.getIntentos());
                    event.setProximoReintento(LocalDateTime.now().plusSeconds(waitSeconds));
                    log.warn("Fallo temporal en evento {}. Intentos: {}. Próximo en {}s. Error: {}", event.getId(), event.getIntentos(), waitSeconds, e.getMessage());
                }
                eventRepository.save(event);
            }
        }
    }
}