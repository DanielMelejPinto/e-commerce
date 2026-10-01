package io.github.danielmelejpinto.usuarioapi.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import io.github.danielmelejpinto.usuarioapi.model.Rol;
import io.github.danielmelejpinto.usuarioapi.model.Usuario;
import io.github.danielmelejpinto.usuarioapi.repository.UsuarioRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuarioControllerTest {

    private static final String URL = "/api/usuarios/registro";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository repository;

    // Email distinto en cada test: ninguno depende de lo que dejó otro
    private String emailUnico() {
        return "user-" + UUID.randomUUID() + "@mail.com";
    }

    private String cuerpo(String nombre, String email, String password) {
        return "{\"nombre\": \"" + nombre + "\", \"email\": \"" + email
                + "\", \"password\": \"" + password + "\"}";
    }

    private ResultActions registrar(String json) throws Exception {
        return mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void registro_conDatosValidos_deberiaRetornar201SinExponerLaClave() throws Exception {
        String email = emailUnico();

        registrar(cuerpo("Ana Pérez", email, "ClaveSegura1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.nombre").value("Ana Pérez"))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.rol").value("USER"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.fechaCreacion").exists())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void registro_deberiaGuardarLaClaveHasheada() throws Exception {
        String email = emailUnico();

        registrar(cuerpo("Ana Pérez", email, "ClaveSegura1")).andExpect(status().isCreated());

        Usuario guardado = repository.findByEmail(email).orElseThrow();
        assertThat(guardado.getPasswordHash()).startsWith("$2").doesNotContain("ClaveSegura1");
    }

    @Test
    void registro_conRolEnElCuerpo_nuncaDeberiaCrearUnAdmin() throws Exception {
        String email = emailUnico();
        String json = "{\"nombre\": \"Intruso\", \"email\": \"" + email
                + "\", \"password\": \"ClaveSegura1\", \"rol\": \"ADMIN\"}";

        MvcResult resultado = registrar(json).andReturn();

        // Según cómo trate Jackson las propiedades desconocidas, puede dar 201 (se ignora
        // el rol) o 400. Lo que jamás puede pasar es que exista un ADMIN
        assertThat(resultado.getResponse().getStatus()).isIn(201, 400);
        repository.findByEmail(email).ifPresent(u -> assertThat(u.getRol()).isEqualTo(Rol.USER));
    }

    @Test
    void registro_conEmailDuplicado_deberiaRetornar409() throws Exception {
        String email = emailUnico();
        registrar(cuerpo("Ana Pérez", email, "ClaveSegura1")).andExpect(status().isCreated());

        registrar(cuerpo("Otra Ana", email, "OtraClave123"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El email ya está registrado"));
    }

    @Test
    void registro_conEmailDuplicadoEnOtraCapitalizacion_deberiaRetornar409() throws Exception {
        String email = emailUnico();
        registrar(cuerpo("Ana Pérez", email, "ClaveSegura1")).andExpect(status().isCreated());

        registrar(cuerpo("Otra Ana", email.toUpperCase(), "OtraClave123"))
                .andExpect(status().isConflict());
    }

    @Test
    void registro_conNombreVacio_deberiaRetornar400() throws Exception {
        registrar(cuerpo("", emailUnico(), "ClaveSegura1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nombre").value("El nombre es obligatorio"));
    }

    @Test
    void registro_conEmailInvalido_deberiaRetornar400() throws Exception {
        registrar(cuerpo("Ana Pérez", "no-es-un-email", "ClaveSegura1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("El email no es válido"));
    }

    @Test
    void registro_conEmailVacio_deberiaRetornar400() throws Exception {
        registrar(cuerpo("Ana Pérez", "", "ClaveSegura1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("El email es obligatorio"));
    }

    @Test
    void registro_sinPassword_deberiaRetornar400() throws Exception {
        registrar("{\"nombre\": \"Ana Pérez\", \"email\": \"" + emailUnico() + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("La contraseña es obligatoria"));
    }

    @Test
    void registro_conPasswordDe7Caracteres_deberiaRetornar400() throws Exception {
        registrar(cuerpo("Ana Pérez", emailUnico(), "Abc1234"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("La contraseña debe tener entre 8 y 72 caracteres"));
    }

    @Test
    void registro_conPasswordDe73Caracteres_deberiaRetornar400() throws Exception {
        registrar(cuerpo("Ana Pérez", emailUnico(), "a".repeat(73)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("La contraseña debe tener entre 8 y 72 caracteres"));
    }

    @Test
    void registro_conPasswordDeMasDe72Bytes_deberiaRetornar400() throws Exception {
        // 40 "ñ": cabe en el límite de caracteres, pero son 80 bytes
        registrar(cuerpo("Ana Pérez", emailUnico(), "ñ".repeat(40)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value(containsString("72 bytes")));
    }

    @Test
    void registro_conJsonMalFormado_deberiaRetornar400() throws Exception {
        registrar("{\"nombre\": \"Ana\", ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El cuerpo de la petición no es un JSON válido"));
    }

    // -------------------------------------------------------------
    // PRUEBAS DE LOGIN
    // -------------------------------------------------------------

    @Test
    void login_conCredencialesCorrectas_deberiaRetornar200YToken() throws Exception {
        String email = emailUnico();
        String password = "MiClaveSecreta123!";

        registrar(cuerpo("Carlos Login", email, password)).andExpect(status().isCreated());

        String jsonLogin = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password);

        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void login_conCredencialesIncorrectas_deberiaRetornar401() throws Exception {
        String email = emailUnico();

        registrar(cuerpo("Carlos Fallo", email, "ClaveCorrecta99")).andExpect(status().isCreated());

        String jsonLogin = """
                {
                    "email": "%s",
                    "password": "ClaveINCORRECTA"
                }
                """.formatted(email);

        mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin))
                .andExpect(status().isUnauthorized()); 
    }

    // -------------------------------------------------------------
    // PRUEBAS DE ENDPOINT PROTEGIDO (/me)
    // -------------------------------------------------------------

    @Test
    void obtenerPerfil_sinToken_deberiaRetornar403() throws Exception {
        // Intenta acceder sin enviar el header Authorization
        mockMvc.perform(get("/api/usuarios/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    void obtenerPerfil_conTokenValido_deberiaRetornar200YDatos() throws Exception {
        String email = emailUnico();
        String password = "ClaveParaToken123!";

        // 1. Registramos
        registrar(cuerpo("Token User", email, password)).andExpect(status().isCreated());

        // 2. Hacemos Login y extraemos el Token
        String jsonLogin = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password);

        MvcResult result = mockMvc.perform(post("/api/usuarios/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonLogin))
                .andExpect(status().isOk())
                .andReturn();

        // Extraer token de la respuesta {"token":"..."}
        String responseBody = result.getResponse().getContentAsString();
        String token = responseBody.split(":")[1].replaceAll("[\"}]", "").trim();

        // 3. Probamos el endpoint protegido con el Token real
        mockMvc.perform(get("/api/usuarios/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Token User"))
                .andExpect(jsonPath("$.email").value(email));
    }
}