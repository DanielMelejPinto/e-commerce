package io.github.danielmelejpinto.pedidoapi.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void manejarProductoNoEncontrado_retorna404() {
        ProductoNoEncontradoException ex = new ProductoNoEncontradoException("Producto 1 no encontrado");
        
        ResponseEntity<Map<String, String>> response = handler.manejarProductoNoEncontrado(ex);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Producto 1 no encontrado");
    }

    @Test
    void manejarStockInsuficiente_retorna409() {
        StockInsuficienteException ex = new StockInsuficienteException("Stock insuficiente para el producto 1");
        
        ResponseEntity<Map<String, String>> response = handler.manejarStockInsuficiente(ex);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Stock insuficiente para el producto 1");
    }

    @Test
    void manejarServicioDependiente_retorna503() {
        ServicioDependienteException ex = new ServicioDependienteException("Inventario caido");
        
        ResponseEntity<Map<String, String>> response = handler.manejarServicioDependiente(ex);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Inventario caido");
    }

    @Test
    void manejarErrorInesperado_retorna500() throws Exception {
        Exception ex = new Exception("Error fatal");
        
        ResponseEntity<Map<String, String>> response = handler.manejarErrorInesperado(ex);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Error interno del servidor");
    }

    @Test
    void manejarDependenciaCliente_retorna400() {
        DependenciaClienteException ex = new DependenciaClienteException("Inventario rechazo por req malo", org.springframework.http.HttpStatus.BAD_REQUEST);
        
        ResponseEntity<Map<String, String>> response = handler.manejarDependenciaCliente(ex);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Inventario rechazo por req malo");
    }
}
