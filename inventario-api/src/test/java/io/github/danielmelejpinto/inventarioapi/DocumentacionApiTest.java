package io.github.danielmelejpinto.inventarioapi;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DocumentacionApiTest {

    @Autowired
    private MockMvc mockMvc;

    // Si springdoc no es compatible con la versión de Spring Boot, este test falla
    @Test
    void openApi_deberiaGenerarseYDescribirLosEndpointsDeInventario() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/inventarios/producto/{productoId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/inventarios/producto/{productoId}'].post").exists())
                .andExpect(jsonPath("$.paths['/api/inventarios/producto/{productoId}'].delete").exists())
                .andExpect(jsonPath("$.paths['/api/inventarios/producto/{productoId}/agregar'].put").exists())
                .andExpect(jsonPath("$.paths['/api/inventarios/producto/{productoId}/reservar'].put").exists());
    }
}