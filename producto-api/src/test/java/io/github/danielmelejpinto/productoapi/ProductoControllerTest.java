package io.github.danielmelejpinto.productoapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
// Reinicia la base de datos entre cada test para que no se pisen entre sí
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class ProductoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // =============================================
    //  POST /api/productos
    // =============================================

    @Test
    void crear_conDatosValidos_deberiaRetornar201() throws Exception {
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"Teclado\", \"precio\": 50.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value("Teclado"))
                .andExpect(jsonPath("$.precio").value(50.00))
                .andExpect(jsonPath("$.fechaCreacion").exists());
    }

    @Test
    void crear_conNombreVacio_deberiaRetornar400() throws Exception {
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"\", \"precio\": 50.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nombre").value("El nombre es obligatorio"));
    }

    @Test
    void crear_conNombreNulo_deberiaRetornar400() throws Exception {
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"precio\": 50.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nombre").value("El nombre es obligatorio"));
    }

    @Test
    void crear_conPrecioNulo_deberiaRetornar400() throws Exception {
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"Teclado\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.precio").value("El precio es obligatorio"));
    }

    @Test
    void crear_conPrecioNegativo_deberiaRetornar400() throws Exception {
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"Teclado\", \"precio\": -10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.precio").value("El precio debe ser mayor a cero"));
    }

    @Test
    void crear_conPrecioCero_deberiaRetornar400() throws Exception {
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"Teclado\", \"precio\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.precio").value("El precio debe ser mayor a cero"));
    }

    // =============================================
    //  GET /api/productos
    // =============================================

    @Test
    void listarTodos_deberiaRetornar200ConListaVacia() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listarTodos_conProductosExistentes_deberiaRetornarLista() throws Exception {
        // Primero creamos un producto
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"Mouse\", \"precio\": 25.00}"));

        // Luego verificamos que aparece en la lista
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Mouse"));
    }

    // =============================================
    //  GET /api/productos/{id}
    // =============================================

    @Test
    void obtenerPorId_conIdExistente_deberiaRetornar200() throws Exception {
        // Creamos el producto y obtenemos el ID
        String respuesta = mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"Monitor\", \"precio\": 300.00}"))
                .andReturn().getResponse().getContentAsString();

        // Verificamos que lo podemos buscar por ID 1
        mockMvc.perform(get("/api/productos/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Monitor"));
    }

    @Test
    void obtenerPorId_conIdInexistente_deberiaRetornar404() throws Exception {
        mockMvc.perform(get("/api/productos/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("El producto no existe"));
    }

    // =============================================
    //  PUT /api/productos/{id}
    // =============================================

    @Test
    void actualizar_conDatosValidos_deberiaRetornar200() throws Exception {
        // Creamos primero
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"Auriculares\", \"precio\": 80.00}"));

        // Actualizamos
        mockMvc.perform(put("/api/productos/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"Auriculares Pro\", \"precio\": 120.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Auriculares Pro"))
                .andExpect(jsonPath("$.precio").value(120.00));
    }

    @Test
    void actualizar_conNombreVacio_deberiaRetornar400() throws Exception {
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"Auriculares\", \"precio\": 80.00}"));

        mockMvc.perform(put("/api/productos/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"\", \"precio\": 120.00}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizar_conIdInexistente_deberiaRetornar404() throws Exception {
        mockMvc.perform(put("/api/productos/999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"Producto\", \"precio\": 50.00}"))
                .andExpect(status().isNotFound());
    }

    // =============================================
    //  DELETE /api/productos/{id}
    // =============================================

    @Test
    void eliminar_conIdExistente_deberiaRetornar204() throws Exception {
        // Creamos primero
        mockMvc.perform(post("/api/productos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"Cable USB\", \"precio\": 10.00}"));

        // Eliminamos y esperamos 204 sin cuerpo
        mockMvc.perform(delete("/api/productos/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminar_conIdInexistente_deberiaRetornar404() throws Exception {
        mockMvc.perform(delete("/api/productos/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("El producto no existe"));
    }
}