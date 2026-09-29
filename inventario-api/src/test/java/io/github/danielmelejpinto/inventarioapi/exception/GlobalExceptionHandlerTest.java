package io.github.danielmelejpinto.inventarioapi.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void manejarConcurrencia_deberiaRetornar409() {
        ObjectOptimisticLockingFailureException ex = new ObjectOptimisticLockingFailureException("Inventario", 1);
        ResponseEntity<Map<String, String>> response = handler.manejarConcurrencia(ex);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("error", "El inventario fue modificado por otra operación, intenta de nuevo");
    }

    @Test
    void manejarIntegridad_deberiaRetornar409() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("Duplicate");
        ResponseEntity<Map<String, String>> response = handler.manejarIntegridad(ex);
        
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("error", "La operación entra en conflicto con datos existentes");
    }
}
