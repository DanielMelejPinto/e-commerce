package io.github.danielmelejpinto.pedidoapi.client;

import io.github.danielmelejpinto.pedidoapi.client.dto.ReservaRequest;
import io.github.danielmelejpinto.pedidoapi.config.ApiProperties;
import io.github.danielmelejpinto.pedidoapi.exception.DependenciaClienteException;
import io.github.danielmelejpinto.pedidoapi.exception.ServicioDependienteException;
import io.github.danielmelejpinto.pedidoapi.exception.StockInsuficienteException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Component
public class InventarioClient {
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InventarioClient(ApiProperties apiProperties, ServiceTokenInterceptor interceptor) {
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(10000);

        this.restClient = RestClient.builder()
                .baseUrl(apiProperties.getInventario().getUrl())
                .requestInterceptor(interceptor)
                .requestFactory(factory)
                .build();
    }

    @CircuitBreaker(name = "inventario", fallbackMethod = "fallbackReservarStock")
    public void reservarStock(Long productoId, Integer cantidad, Long pedidoId) {
        restClient.put()
                .uri("/producto/{productoId}/reservar", productoId)
                .body(new ReservaRequest(cantidad, pedidoId))
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
                    throw new DependenciaClienteException("Error de cliente en inventario (reservar): " + response.getStatusCode(), response.getStatusCode());
                })
                .onStatus(status -> status.is5xxServerError(), (request, response) -> {
                    throw new ServicioDependienteException("Error del servidor en inventario (reservar)");
                })
                .toBodilessEntity();
    }

    @CircuitBreaker(name = "inventario-liberar", fallbackMethod = "fallbackLiberarStock")
    public void liberarStock(Long productoId, Integer cantidad, Long pedidoId) {
        restClient.put()
                .uri("/producto/{productoId}/liberar", productoId)
                .body(new ReservaRequest(cantidad, pedidoId))
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), (request, response) -> {
                    throw new DependenciaClienteException("Error de cliente en inventario (liberar): " + response.getStatusCode(), response.getStatusCode());
                })
                .onStatus(status -> status.is5xxServerError(), (request, response) -> {
                    throw new ServicioDependienteException("Error del servidor en inventario (liberar)");
                })
                .toBodilessEntity();
    }

    public void fallbackReservarStock(Long productoId, Integer cantidad, Long pedidoId, Throwable t) {
        if (t instanceof StockInsuficienteException || t instanceof DependenciaClienteException) {
            throw (RuntimeException) t; // errores de negocio/cliente: no son "servicio caído"
        }
        throw new ServicioDependienteException("El servicio de inventario está inactivo. Fallback activado (Circuit Breaker).");
    }

    public void fallbackLiberarStock(Long productoId, Integer cantidad, Long pedidoId, Throwable t) {
        if (t instanceof DependenciaClienteException) {
            throw (RuntimeException) t;
        }
        throw new ServicioDependienteException("El servicio de inventario está inactivo. Fallback activado (Circuit Breaker).");
    }
}
