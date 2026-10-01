package io.github.danielmelejpinto.pedidoapi.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class PedidoItemRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void cantidadEnElLimite_esValida() {
        assertThat(validator.validate(new PedidoItemRequest(1L, 100000))).isEmpty();
    }

    @Test
    void cantidadSobreElLimite_daMensajeClaro() {
        Set<ConstraintViolation<PedidoItemRequest>> v = validator.validate(new PedidoItemRequest(1L, 100001));
        assertThat(v).hasSize(1);
        assertThat(v.iterator().next().getMessage()).isEqualTo("La cantidad no puede superar 100000");
    }

    @Test
    void cantidadCero_sigueSiendoInvalida() {
        assertThat(validator.validate(new PedidoItemRequest(1L, 0))).hasSize(1);
    }
}
