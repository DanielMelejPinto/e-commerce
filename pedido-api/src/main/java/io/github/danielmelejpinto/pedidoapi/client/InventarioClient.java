package io.github.danielmelejpinto.pedidoapi.client;

import io.github.danielmelejpinto.pedidoapi.client.dto.ReservaRequest;
import io.github.danielmelejpinto.pedidoapi.exception.ServicioDependienteException;
import io.github.danielmelejpinto.pedidoapi.exception.StockInsuficienteException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;

@Component
public class InventarioClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InventarioClient(@Value("${api.inventario.url}") String url) {
        this.restClient = RestClient.builder().baseUrl(url).build();
    }

    public void reservarStock(Long productoId, Integer cantidad) {
        restClient.put()
                .uri("/producto/{productoId}/reservar", productoId)
                .body(new ReservaRequest(cantidad))
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), (request, response) -> {
                    if (response.getStatusCode().value() == 409) {
                        String body = new String(response.getBody().readAllBytes());
                        try {
                            JsonNode node = objectMapper.readTree(body);
                            if (node.has("error")) {
                                throw new StockInsuficienteException(node.get("error").asText());
                            }
                        } catch (IOException ignored) {}
                        throw new StockInsuficienteException("Stock insuficiente para el producto " + productoId);
                    }
                    throw new ServicioDependienteException("Error de cliente en inventario (reservar): " + response.getStatusCode());
                })
                .onStatus(status -> status.is5xxServerError(), (request, response) -> {
                    throw new ServicioDependienteException("Error del servidor en inventario (reservar)");
                })
                .toBodilessEntity();
    }

    public void liberarStock(Long productoId, Integer cantidad) {
        restClient.put()
                .uri("/producto/{productoId}/liberar", productoId)
                .body(new ReservaRequest(cantidad))
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), (request, response) -> {
                    throw new ServicioDependienteException("Error de cliente en inventario (liberar): " + response.getStatusCode());
                })
                .onStatus(status -> status.is5xxServerError(), (request, response) -> {
                    throw new ServicioDependienteException("Error del servidor en inventario (liberar)");
                })
                .toBodilessEntity();
    }
}
