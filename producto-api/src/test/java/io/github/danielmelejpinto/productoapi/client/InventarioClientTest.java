package io.github.danielmelejpinto.productoapi.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import io.github.danielmelejpinto.productoapi.exception.InventarioNoDisponibleException;
import io.github.danielmelejpinto.productoapi.exception.InventarioRechazoException;

class InventarioClientTest {

    private MockRestServiceServer mockServer;
    private InventarioClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://inventario.test");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new InventarioClient(builder.build());
    }

    @Test
    void inicializarInventario_conRespuesta2xx_deberiaCompletarExitosamente() {
        mockServer.expect(requestTo("http://inventario.test/api/inventarios/producto/1"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess());

        client.inicializarInventario(1L);

        mockServer.verify();
    }

    @Test
    void inicializarInventario_conRespuesta4xx_deberiaLanzarInventarioRechazoException() {
        mockServer.expect(requestTo("http://inventario.test/api/inventarios/producto/2"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> client.inicializarInventario(2L))
                .isInstanceOf(InventarioRechazoException.class)
                .hasMessageContaining("El inventario rechazó la petición (4xx) para el producto 2");

        mockServer.verify();
    }

    @Test
    void inicializarInventario_conRespuesta5xx_deberiaLanzarInventarioNoDisponibleException() {
        mockServer.expect(requestTo("http://inventario.test/api/inventarios/producto/3"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.inicializarInventario(3L))
                .isInstanceOf(InventarioNoDisponibleException.class)
                .hasMessageContaining("El inventario devolvió error de servidor (5xx) para el producto 3");

        mockServer.verify();
    }
}
