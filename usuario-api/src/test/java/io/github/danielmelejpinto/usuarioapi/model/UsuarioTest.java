package io.github.danielmelejpinto.usuarioapi.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void setEmail_deberiaNormalizarAMinusculasYQuitarEspacios() {
        Usuario usuario = new Usuario();
        usuario.setEmail("  Ana.Perez@Mail.COM ");

        assertThat(usuario.getEmail()).isEqualTo("ana.perez@mail.com");
    }

    @Test
    void setEmail_conNull_deberiaQuedarNulo() {
        Usuario usuario = new Usuario();
        usuario.setEmail(null);

        assertThat(usuario.getEmail()).isNull();
    }

    @Test
    void nuevoUsuario_deberiaTenerRolUserYEstadoActivoPorDefecto() {
        Usuario usuario = new Usuario();

        assertThat(usuario.getRol()).isEqualTo(Rol.USER);
        assertThat(usuario.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
    }
}