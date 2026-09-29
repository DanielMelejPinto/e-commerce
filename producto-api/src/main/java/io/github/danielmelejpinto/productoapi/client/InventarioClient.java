package io.github.danielmelejpinto.productoapi.client;

import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import io.github.danielmelejpinto.productoapi.exception.InventarioNoDisponibleException;
import io.github.danielmelejpinto.productoapi.exception.InventarioRechazoException;

@Component
public class InventarioClient {

    private final RestClient restClient;

    public InventarioClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public void inicializarInventario(Long productoId) {
        try {
            restClient.post()
                    .uri("/api/inventarios/producto/{id}", productoId)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), (request, response) -> {
                        throw new InventarioRechazoException("El inventario rechazó la petición (4xx) para el producto " + productoId);
                    })
                    .onStatus(status -> status.is5xxServerError(), (request, response) -> {
                        throw new InventarioNoDisponibleException("El inventario devolvió error de servidor (5xx) para el producto " + productoId, null);
                    })
                    .toBodilessEntity();
        } catch (InventarioRechazoException | InventarioNoDisponibleException e) {
            throw e; // Relanzar las custom exceptions mapeadas en onStatus
        } catch (Exception e) {
            // timeout u otro error de red
            throw new InventarioNoDisponibleException("Error de red al comunicarse con inventario-api", e);
        }
    }
}
