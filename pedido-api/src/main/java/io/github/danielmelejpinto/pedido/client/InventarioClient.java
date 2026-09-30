package io.github.danielmelejpinto.pedido.client;

import io.github.danielmelejpinto.pedido.client.dto.ReservaRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class InventarioClient {
    private final RestClient restClient;

    public InventarioClient(RestClient.Builder builder, @Value("${api.inventario.url}") String url) {
        this.restClient = builder.baseUrl(url).build();
    }

    public void reservarStock(Long productoId, Integer cantidad) {
        restClient.put()
                .uri("/producto/{productoId}/reservar", productoId)
                .body(new ReservaRequest(cantidad))
                .retrieve()
                .toBodilessEntity();
    }

        public void liberarStock(Long productoId, Integer cantidad) {
        restClient.put()
                .uri("/producto/{productoId}/liberar", productoId)
                .body(new ReservaRequest(cantidad))
                .retrieve()
                .toBodilessEntity();
    }
}
