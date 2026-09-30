package io.github.danielmelejpinto.pedidoapi.client;

import io.github.danielmelejpinto.pedidoapi.client.dto.ProductoDTO;
import io.github.danielmelejpinto.pedidoapi.config.ApiProperties;
import io.github.danielmelejpinto.pedidoapi.exception.ProductoNoEncontradoException;
import io.github.danielmelejpinto.pedidoapi.exception.ServicioDependienteException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProductoClient {
    private final RestClient restClient;

    public ProductoClient(ApiProperties apiProperties) {
        this.restClient = RestClient.builder().baseUrl(apiProperties.getProducto().getUrl()).build();
    }

    public ProductoDTO obtenerProducto(Long id) {
        return restClient.get()
                .uri("/{id}", id)
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), (request, response) -> {
                    if (response.getStatusCode().value() == 404) {
                        throw new ProductoNoEncontradoException("Producto no encontrado: " + id);
                    }
                    throw new ServicioDependienteException("Error de cliente al consultar producto " + id + ": " + response.getStatusCode());
                })
                .onStatus(status -> status.is5xxServerError(), (request, response) -> {
                    throw new ServicioDependienteException("Error del servidor al consultar producto " + id);
                })
                .body(ProductoDTO.class);
    }
}
