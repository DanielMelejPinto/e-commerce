package io.github.danielmelejpinto.productoapi.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class ProductoControllerTest {

    private static final String URL = "/api/productos";

    @Autowired
    private MockMvc mockMvc;

    // Sustituye al RestTemplate real: los tests no dependen de que inventario-api esté corriendo
    @MockitoBean
    private RestTemplate restTemplate;

    // =============================================
    // Helpers
    // =============================================

    // Arma el JSON de un producto para no repetir comillas escapadas
    private String cuerpo(String nombre, String precio) {
        return "{\"nombre\": \"" + nombre + "\", \"precio\": " + precio + "}";
    }

    // Crea un producto vía API y devuelve la respuesta completa (para leer id y
    // headers)
    private MvcResult crearYObtenerResultado(String nombre, String precio) throws Exception {
        return mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(nombre, precio)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private long leerId(MvcResult resultado) throws Exception {
        String json = resultado.getResponse().getContentAsString();
        // JsonPath puede devolver Integer o Long según el tamaño, por eso Number
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    // Crea un producto y devuelve solo su id: así ningún test depende de "ID 1"
    private long crearProducto(String nombre, String precio) throws Exception {
        return leerId(crearYObtenerResultado(nombre, precio));
    }

    // =============================================
    // POST /api/productos
    // =============================================

    @Test
    void crear_conDatosValidos_deberiaRetornar201() throws Exception {
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("Teclado", "50.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value("Teclado"))
                .andExpect(jsonPath("$.precio").value(50.00))
                .andExpect(jsonPath("$.fechaCreacion").exists());
    }

    @Test
    void crear_deberiaDevolverHeaderLocationConLaUrlDelRecurso() throws Exception {
        MvcResult resultado = crearYObtenerResultado("Teclado", "50.00");
        long id = leerId(resultado);

        String location = resultado.getResponse().getHeader("Location");
        assertEquals("http://localhost" + URL + "/" + id, location);
    }

    @Test
    void crear_conNombreVacio_deberiaRetornar400() throws Exception {
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("", "50.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nombre").value("El nombre es obligatorio"));
    }

    @Test
    void crear_conNombreNulo_deberiaRetornar400() throws Exception {
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"precio\": 50.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nombre").value("El nombre es obligatorio"));
    }

    @Test
    void crear_conNombreDe151Caracteres_deberiaRetornar400() throws Exception {
        String nombreLargo = "a".repeat(151);

        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo(nombreLargo, "50.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nombre").value("El nombre no puede superar 150 caracteres"));
    }

    @Test
    void crear_conPrecioNulo_deberiaRetornar400() throws Exception {
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"Teclado\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.precio").value("El precio es obligatorio"));
    }

    @Test
    void crear_conPrecioNegativo_deberiaRetornar400() throws Exception {
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("Teclado", "-10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.precio").value("El precio debe ser mayor a cero"));
    }

    @Test
    void crear_conPrecioCero_deberiaRetornar400() throws Exception {
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("Teclado", "0")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.precio").value("El precio debe ser mayor a cero"));
    }

    @Test
    void crear_conMasDeDosDecimales_deberiaRetornar400() throws Exception {
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("Teclado", "10.123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.precio").value("El precio admite hasta 10 enteros y 2 decimales"));
    }

    @Test
    void crear_conMasDeDiezEnteros_deberiaRetornar400() throws Exception {
        // 11 dígitos enteros: antes de @Digits esto provocaba un 500 en la base de
        // datos
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("Teclado", "12345678901")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.precio").value("El precio admite hasta 10 enteros y 2 decimales"));
    }

    @Test
    void crear_conJsonMalFormado_deberiaRetornar400() throws Exception {
        // Falta cerrar la llave y el valor de precio
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\": \"Teclado\", "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // =============================================
    // GET /api/productos
    // =============================================

    @Test
    void listarTodos_deberiaRetornar200ConPaginaDeResultados() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page.size").value(10))
                .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    void listarTodos_conProductosExistentes_deberiaIncluirElProductoCreado() throws Exception {
        crearProducto("Mouse", "25.00");

        // sort=id,desc: el más reciente sale primero, así no depende de cuántos
        // productos haya
        mockMvc.perform(get(URL).param("sort", "id,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.nombre == 'Mouse')]").exists());
    }

    @Test
    void listarTodos_conSizeDos_deberiaDevolverSoloDosElementos() throws Exception {
        crearProducto("Pag1", "10.00");
        crearProducto("Pag2", "10.00");
        crearProducto("Pag3", "10.00");

        mockMvc.perform(get(URL).param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page.size").value(2))
                .andExpect(jsonPath("$.page.totalPages").isNumber());
    }

    @Test
    void listarTodos_ordenadoPorPrecioDescendente_deberiaDevolverElMasCaroPrimero() throws Exception {
        // 9999999999.99 es el precio máximo permitido: ningún otro test lo usa
        crearProducto("MasCaro", "9999999999.99");

        mockMvc.perform(get(URL).param("sort", "precio,desc").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nombre").value("MasCaro"));
    }

    @Test
    void listarTodos_conCampoDeOrdenInvalido_deberiaRetornar400() throws Exception {
        mockMvc.perform(get(URL).param("sort", "campoInventado"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void listarTodos_conSizeExcesivo_deberiaLimitarseAlMaximoPermitido() throws Exception {
        mockMvc.perform(get(URL).param("size", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.size").value(50));
    }

    // =============================================
    // GET /api/productos/{id}
    // =============================================

    @Test
    void obtenerPorId_conIdExistente_deberiaRetornar200() throws Exception {
        long id = crearProducto("Monitor", "300.00");

        mockMvc.perform(get(URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nombre").value("Monitor"));
    }

    @Test
    void obtenerPorId_conIdInexistente_deberiaRetornar404() throws Exception {
        mockMvc.perform(get(URL + "/999999"))
                .andExpect(status().isNotFound())
                // CAMBIO: el mensaje ahora incluye el id
                .andExpect(jsonPath("$.error").value("Producto con id 999999 no existe"));
    }

    @Test
    void obtenerPorId_conIdNoNumerico_deberiaRetornar400() throws Exception {
        mockMvc.perform(get(URL + "/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    // =============================================
    // PUT /api/productos/{id}
    // =============================================

    @Test
    void actualizar_conDatosValidos_deberiaRetornar200() throws Exception {
        long id = crearProducto("Auriculares", "80.00");

        mockMvc.perform(put(URL + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("Auriculares Pro", "120.00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Auriculares Pro"))
                .andExpect(jsonPath("$.precio").value(120.00));
    }

    @Test
    void actualizar_conNombreVacio_deberiaRetornar400() throws Exception {
        long id = crearProducto("Auriculares", "80.00");

        mockMvc.perform(put(URL + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("", "120.00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void actualizar_conIdInexistente_deberiaRetornar404() throws Exception {
        mockMvc.perform(put(URL + "/999999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo("Producto", "50.00")))
                .andExpect(status().isNotFound());
    }

    // =============================================
    // DELETE /api/productos/{id}
    // =============================================

    @Test
    void eliminar_conIdExistente_deberiaRetornar204YYaNoExistir() throws Exception {
        long id = crearProducto("Cable USB", "10.00");

        mockMvc.perform(delete(URL + "/" + id))
                .andExpect(status().isNoContent());

        // Comprobamos que de verdad se borró
        mockMvc.perform(get(URL + "/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminar_conIdInexistente_deberiaRetornar404() throws Exception {
        mockMvc.perform(delete(URL + "/999999"))
                .andExpect(status().isNotFound())
                // CAMBIO: el mensaje ahora incluye el id
                .andExpect(jsonPath("$.error").value("Producto con id 999999 no existe"));
    }
}