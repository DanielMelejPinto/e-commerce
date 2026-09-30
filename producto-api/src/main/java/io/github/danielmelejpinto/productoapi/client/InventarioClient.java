package io.github.danielmelejpinto.productoapi.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import io.github.danielmelejpinto.productoapi.exception.InventarioNoDisponibleException;
import io.github.danielmelejpinto.productoapi.exception.InventarioRechazoException;

import io.github.danielmelejpinto.productoapi.security.JwtService;

@Component
public class InventarioClient {

    private final RestClient restClient;
    private final JwtService jwtService;

    public InventarioClient(RestClient restClient, JwtService jwtService) {
        this.restClient = restClient;
        this.jwtService = jwtService;
    }

    public void inicializarInventario(Long productoId) {
        try {
            String token = jwtService.generarTokenSistema();
            restClient.post()
                    .uri("/api/inventarios/producto/{id}", productoId)
                    .header("Authorization", "Bearer " + token)
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
