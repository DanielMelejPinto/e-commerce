package io.github.danielmelejpinto.inventarioapi.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductoEventListenerTest {

    @Mock
    private InventarioService inventarioService;

    @InjectMocks
    private ProductoEventListener listener;

    @Test
    void eventoValido_deberiaInicializarElInventario() {
        listener.onProductoCreated(Map.of("id", 7));

        verify(inventarioService).inicializarInventario(7L);
    }

    @Test
    void siElServicioFalla_deberiaRelanzarParaQueKafkaReintente() {
        when(inventarioService.inicializarInventario(7L)).thenThrow(new IllegalStateException("bd caida"));

        assertThatThrownBy(() -> listener.onProductoCreated(Map.of("id", 7)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void eventoSinId_noDeberiaLlamarAlServicioNiFallar() {
        listener.onProductoCreated(Map.of("otro", 1));

        verifyNoInteractions(inventarioService);
    }
}
