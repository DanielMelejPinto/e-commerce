package io.github.danielmelejpinto.pedidoapi.client;

import io.github.danielmelejpinto.pedidoapi.client.dto.ProductoDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ProductoClient {
    private final RestClient restClient;

    public ProductoClient(RestClient.Builder builder, @Value("${api.producto.url}") String url) {
        this.restClient = builder.baseUrl(url).build();
    }

    public ProductoDTO obtenerProducto(Long id) {
        return restClient.get()
                .uri("/{id}", id)
                .retrieve()
                .body(ProductoDTO.class);
    }
}
