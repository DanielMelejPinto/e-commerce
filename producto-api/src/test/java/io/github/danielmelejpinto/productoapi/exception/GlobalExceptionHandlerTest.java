package io.github.danielmelejpinto.productoapi.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import io.github.danielmelejpinto.productoapi.model.Producto;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void manejarConcurrencia_deberiaRetornar409YMensajeDeError() {
        ObjectOptimisticLockingFailureException ex = new ObjectOptimisticLockingFailureException(Producto.class, 1L);
        
        ResponseEntity<Map<String, String>> response = handler.manejarConcurrencia(ex);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsKey("error");
        assertThat(response.getBody().get("error")).isEqualTo("El producto fue modificado por otra operación, intenta de nuevo");
    }
}
