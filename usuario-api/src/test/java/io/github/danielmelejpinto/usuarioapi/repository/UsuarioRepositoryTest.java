package io.github.danielmelejpinto.usuarioapi.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import io.github.danielmelejpinto.usuarioapi.model.EstadoUsuario;
import io.github.danielmelejpinto.usuarioapi.model.Usuario;

@DataJpaTest
@ActiveProfiles("test")
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository repository;

    // Usuario válido mínimo; el hash es de mentira (aquí no se valida nada de BCrypt)
    private Usuario nuevo(String email) {
        Usuario usuario = new Usuario();
        usuario.setEmail(email);
        usuario.setPasswordHash("$2a$12$hashDePrueba");
        usuario.setNombre("Usuario de prueba");
        return usuario;
    }

    @Test
    void guardar_deberiaAsignarIdYFechaDeCreacion() {
        Usuario guardado = repository.saveAndFlush(nuevo("ana@mail.com"));

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getFechaCreacion()).isNotNull();
    }

    @Test
    void actualizar_deberiaIncrementarLaVersion() {
        Usuario guardado = repository.saveAndFlush(nuevo("ana@mail.com"));
        assertThat(guardado.getVersion()).isZero();

        guardado.setNombre("Ana Actualizada");
        repository.flush();

        assertThat(guardado.getVersion()).isEqualTo(1L);
    }

    @Test
    void findByEmail_conEmailExistente_deberiaDevolverElUsuario() {
        repository.saveAndFlush(nuevo("ana@mail.com"));

        assertThat(repository.findByEmail("ana@mail.com")).isPresent();
    }

    @Test
    void findByEmail_conEmailInexistente_deberiaDevolverVacio() {
        assertThat(repository.findByEmail("nadie@mail.com")).isEmpty();
    }

    @Test
    void existsByEmail_deberiaReflejarSiExiste() {
        repository.saveAndFlush(nuevo("ana@mail.com"));

        assertThat(repository.existsByEmail("ana@mail.com")).isTrue();
        assertThat(repository.existsByEmail("otro@mail.com")).isFalse();
    }

    @Test
    void guardar_conEmailDuplicado_deberiaLanzarExcepcion() {
        repository.saveAndFlush(nuevo("ana@mail.com"));

        assertThatThrownBy(() -> repository.saveAndFlush(nuevo("ana@mail.com")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void guardar_conEmailDuplicadoEnOtraCapitalizacion_deberiaLanzarExcepcion() {
        repository.saveAndFlush(nuevo("ana@mail.com"));

        assertThatThrownBy(() -> repository.saveAndFlush(nuevo(" ANA@Mail.com ")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByEstado_deberiaDevolverSoloLosUsuariosDeEseEstado() {
        Usuario activo = nuevo("activo@mail.com");
        Usuario baja = nuevo("baja@mail.com");
        baja.setEstado(EstadoUsuario.BAJA);
        repository.saveAll(List.of(activo, baja));

        Page<Usuario> pagina = repository.findByEstado(EstadoUsuario.ACTIVO, PageRequest.of(0, 10));

        assertThat(pagina.getContent()).extracting(Usuario::getEmail).containsExactly("activo@mail.com");
    }
}