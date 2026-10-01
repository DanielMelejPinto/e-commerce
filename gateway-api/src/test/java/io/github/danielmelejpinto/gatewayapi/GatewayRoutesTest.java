package io.github.danielmelejpinto.gatewayapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;

@SpringBootTest
class GatewayRoutesTest {

    @Autowired
    private RouteLocator routeLocator;

    @Test
    void cargaLasRutasConfiguradas() {
        List<Route> routes = routeLocator.getRoutes().collectList().block();
        assertThat(routes).extracting(Route::getId)
                .containsExactlyInAnyOrder("producto-api", "inventario-api", "usuario-api", "pedido-api");
    }

    @Test
    void cadaRutaApuntaASuServicioPorDefecto() {
        Map<String, String> uris = routeLocator.getRoutes().collectList().block().stream()
                .collect(Collectors.toMap(Route::getId, r -> r.getUri().toString()));
        assertThat(uris).containsEntry("producto-api", "http://localhost:8080")
                .containsEntry("inventario-api", "http://localhost:8081")
                .containsEntry("usuario-api", "http://localhost:8082")
                .containsEntry("pedido-api", "http://localhost:8083");
    }
}
