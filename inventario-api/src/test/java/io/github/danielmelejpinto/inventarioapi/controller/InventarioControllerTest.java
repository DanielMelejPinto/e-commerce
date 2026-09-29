package io.github.danielmelejpinto.inventarioapi.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final AtomicLong NEXT_ID = new AtomicLong(1000L);

    @Test
    void obtener_conInventarioExistente_deberiaRetornar200() throws Exception {
        Long productoId = NEXT_ID.getAndIncrement();
        
        mockMvc.perform(post("/api/inventarios/producto/{id}", productoId))
                .andExpect(status().isCreated());
                
        mockMvc.perform(get("/api/inventarios/producto/{id}", productoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productoId").value(productoId))
                .andExpect(jsonPath("$.cantidadDisponible").value(0))
                .andExpect(jsonPath("$.cantidadReservada").value(0));
    }

    @Test
    void obtener_sinInventario_deberiaRetornar404() throws Exception {
        Long productoId = NEXT_ID.getAndIncrement();
        
        mockMvc.perform(get("/api/inventarios/producto/{id}", productoId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No existe inventario para el producto con id " + productoId));
    }

    @Test
    void obtener_conIdNoNumerico_deberiaRetornar400() throws Exception {
        mockMvc.perform(get("/api/inventarios/producto/abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inicializar_deberiaRetornar201YLuegoAlLlamarloDeNuevoRetornarElMismo() throws Exception {
        Long productoId = NEXT_ID.getAndIncrement();
        
        mockMvc.perform(post("/api/inventarios/producto/{id}", productoId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productoId").value(productoId))
                .andExpect(jsonPath("$.cantidadDisponible").value(0))
                .andExpect(jsonPath("$.cantidadReservada").value(0))
                .andExpect(header().exists("Location"));
                
        mockMvc.perform(post("/api/inventarios/producto/{id}", productoId))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productoId").value(productoId));
    }

    @Test
    void agregar_conDatosValidos_deberiaRetornar200YSumarAcumulativamente() throws Exception {
        Long productoId = NEXT_ID.getAndIncrement();
        
        mockMvc.perform(post("/api/inventarios/producto/{id}", productoId))
                .andExpect(status().isCreated());
                
        mockMvc.perform(put("/api/inventarios/producto/{id}/agregar", productoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\": 5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadDisponible").value(5));
                
        mockMvc.perform(put("/api/inventarios/producto/{id}/agregar", productoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\": 10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadDisponible").value(15));
    }

    @Test
    void agregar_conCantidadesInvalidas_deberiaRetornar400() throws Exception {
        Long productoId = NEXT_ID.getAndIncrement();
        
        mockMvc.perform(put("/api/inventarios/producto/{id}/agregar", productoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cantidad").value("La cantidad debe ser mayor a cero"));
                
        mockMvc.perform(put("/api/inventarios/producto/{id}/agregar", productoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\": -5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cantidad").value("La cantidad debe ser mayor a cero"));
                
        mockMvc.perform(put("/api/inventarios/producto/{id}/agregar", productoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cantidad").value("La cantidad es obligatoria"));
                
        mockMvc.perform(put("/api/inventarios/producto/{id}/agregar", productoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\": 100001}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.cantidad").value("La cantidad no puede superar 100000 unidades por operación"));
                
        mockMvc.perform(put("/api/inventarios/producto/{id}/agregar", productoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\": \"no-numero\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void agregar_sinInventario_deberiaRetornar404() throws Exception {
        Long productoId = NEXT_ID.getAndIncrement();
        
        mockMvc.perform(put("/api/inventarios/producto/{id}/agregar", productoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\": 5}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void reservar_conStockSuficiente_deberiaMoverStockYRetornar200() throws Exception {
        Long productoId = NEXT_ID.getAndIncrement();
        
        mockMvc.perform(post("/api/inventarios/producto/{id}", productoId));
        mockMvc.perform(put("/api/inventarios/producto/{id}/agregar", productoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\": 10}"));
                
        mockMvc.perform(put("/api/inventarios/producto/{id}/reservar", productoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\": 3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadDisponible").value(7))
                .andExpect(jsonPath("$.cantidadReservada").value(3));
    }

    @Test
    void reservar_conStockInsuficiente_deberiaRetornar409() throws Exception {
        Long productoId = NEXT_ID.getAndIncrement();
        
        mockMvc.perform(post("/api/inventarios/producto/{id}", productoId));
        mockMvc.perform(put("/api/inventarios/producto/{id}/agregar", productoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\": 5}"));
                
        mockMvc.perform(put("/api/inventarios/producto/{id}/reservar", productoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\": 10}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Stock insuficiente para el producto " + productoId + ": disponible 5, solicitado 10"));
    }

    @Test
    void reservar_conCantidadInvalida_deberiaRetornar400() throws Exception {
        Long productoId = NEXT_ID.getAndIncrement();
        
        mockMvc.perform(put("/api/inventarios/producto/{id}/reservar", productoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\": -1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reservar_sinInventario_deberiaRetornar404() throws Exception {
        Long productoId = NEXT_ID.getAndIncrement();
        
        mockMvc.perform(put("/api/inventarios/producto/{id}/reservar", productoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\": 5}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_deberiaRetornar204YBorrarElInventarioSiExiste() throws Exception {
        Long productoId = NEXT_ID.getAndIncrement();
        
        mockMvc.perform(post("/api/inventarios/producto/{id}", productoId));
        
        mockMvc.perform(delete("/api/inventarios/producto/{id}", productoId))
                .andExpect(status().isNoContent());
                
        mockMvc.perform(get("/api/inventarios/producto/{id}", productoId))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_sinInventario_deberiaRetornar204() throws Exception {
        Long productoId = NEXT_ID.getAndIncrement();
        
        mockMvc.perform(delete("/api/inventarios/producto/{id}", productoId))
                .andExpect(status().isNoContent());
    }

    @Test
    void concurrencia_dosReservasSimultaneasSobreStockUno_deberiaPermitirSoloUnaYLaOtraLanzar409() throws Exception {
        Long productoId = NEXT_ID.getAndIncrement();
        
        mockMvc.perform(post("/api/inventarios/producto/{id}", productoId));
        mockMvc.perform(put("/api/inventarios/producto/{id}/agregar", productoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"cantidad\": 1}"));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(2);

        AtomicInteger status200 = new AtomicInteger(0);
        AtomicInteger status409 = new AtomicInteger(0);

        Runnable task = () -> {
            try {
                latch.await();
                MvcResult result = mockMvc.perform(put("/api/inventarios/producto/{id}/reservar", productoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\": 1}"))
                        .andReturn();
                        
                int status = result.getResponse().getStatus();
                if (status == 200) status200.incrementAndGet();
                if (status == 409) status409.incrementAndGet();
            } catch (Exception ignored) {
            } finally {
                endLatch.countDown();
            }
        };

        executor.submit(task);
        executor.submit(task);

        latch.countDown();
        endLatch.await();
        executor.shutdown();

        assertThat(status200.get()).isEqualTo(1);
        assertThat(status409.get()).isEqualTo(1);

        mockMvc.perform(get("/api/inventarios/producto/{id}", productoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadDisponible").value(0))
                .andExpect(jsonPath("$.cantidadReservada").value(1));
    }
}
