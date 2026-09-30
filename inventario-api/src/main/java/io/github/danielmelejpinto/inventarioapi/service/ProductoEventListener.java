package io.github.danielmelejpinto.inventarioapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProductoEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProductoEventListener.class);
    
    private final InventarioService inventarioService;

    public ProductoEventListener(InventarioService inventarioService) {
        this.inventarioService = inventarioService;
    }

    @KafkaListener(topics = "producto-events", groupId = "inventario-group")
    public void onProductoCreated(Map<String, Object> payload) {
        try {
            // El OutboxProcessor de producto-api envía el objeto Producto como JSON
            if (payload.containsKey("id")) {
                Long productoId = ((Number) payload.get("id")).longValue();
                log.info("Evento Kafka recibido: Inicializando inventario para el producto {}", productoId);
                inventarioService.inicializarInventario(productoId);
            } else {
                log.warn("Evento Kafka recibido sin ID de producto: {}", payload);
            }
        } catch (Exception e) {
            log.error("Error al procesar evento Kafka: {}", e.getMessage());
            // En un caso real, podríamos enviar esto a un Dead Letter Queue (DLQ)
        }
    }
}
