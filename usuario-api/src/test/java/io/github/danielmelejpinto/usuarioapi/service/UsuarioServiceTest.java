package io.github.danielmelejpinto.usuarioapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import io.github.danielmelejpinto.usuarioapi.dto.RegistroRequest;
import io.github.danielmelejpinto.usuarioapi.dto.UsuarioResponse;
import io.github.danielmelejpinto.usuarioapi.exception.CampoInvalidoException;
import io.github.danielmelejpinto.usuarioapi.exception.EmailYaRegistradoException;
import io.github.danielmelejpinto.usuarioapi.model.EstadoUsuario;
import io.github.danielmelejpinto.usuarioapi.model.Rol;
import io.github.danielmelejpinto.usuarioapi.model.Usuario;
import io.github.danielmelejpinto.usuarioapi.repository.UsuarioRepository;

import org.springframework.security.authentication.AuthenticationManager;
import io.github.danielmelejpinto.usuarioapi.security.JwtService;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository repository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    // BCrypt real pero con costo mínimo, para poder comprobar el hash de verdad
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(repository, passwordEncoder, authenticationManager, jwtService);
    }

    private RegistroRequest request(String email, String password) {
        return new RegistroRequest("Ana Pérez", email, password);
    }

    @Test
    void registrar_conDatosValidos_deberiaGuardarConHashYRolUser() {
        when(repository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        UsuarioResponse response = service.registrar(request("ana@mail.com", "ClaveSegura1"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).saveAndFlush(captor.capture());
        Usuario guardado = captor.getValue();
        assertThat(guardado.getPasswordHash()).isNotEqualTo("ClaveSegura1");
        assertThat(passwordEncoder.matches("ClaveSegura1", guardado.getPasswordHash())).isTrue();
        assertThat(guardado.getRol()).isEqualTo(Rol.USER);
        assertThat(guardado.getEstado()).isEqualTo(EstadoUsuario.ACTIVO);
        assertThat(response.email()).isEqualTo("ana@mail.com");
        assertThat(response.rol()).isEqualTo(Rol.USER);
    }

    @Test
    void registrar_deberiaNormalizarElEmailYRecortarElNombre() {
        when(repository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        service.registrar(new RegistroRequest("  Ana Pérez ", "  ANA@Mail.COM ", "ClaveSegura1"));

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(repository).existsByEmail("ana@mail.com");
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("ana@mail.com");
        assertThat(captor.getValue().getNombre()).isEqualTo("Ana Pérez");
    }

    @Test
    void registrar_conEmailExistente_deberiaLanzarEmailYaRegistradoYNoGuardar() {
        when(repository.existsByEmail("ana@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(request("ana@mail.com", "ClaveSegura1")))
                .isInstanceOf(EmailYaRegistradoException.class);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void registrar_cuandoLaBaseRechazaPorDuplicado_deberiaLanzarEmailYaRegistrado() {
        // Carrera: existsByEmail dijo que no, pero otro registro llegó primero
        when(repository.saveAndFlush(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThatThrownBy(() -> service.registrar(request("ana@mail.com", "ClaveSegura1")))
                .isInstanceOf(EmailYaRegistradoException.class);
    }

    @Test
    void registrar_conPasswordDeMasDe72Bytes_deberiaLanzarCampoInvalido() {
        // 40 "ñ" = 40 caracteres pero 80 bytes
        String passwordLarga = "ñ".repeat(40);

        assertThatThrownBy(() -> service.registrar(request("ana@mail.com", passwordLarga)))
                .isInstanceOf(CampoInvalidoException.class)
                .satisfies(ex -> assertThat(((CampoInvalidoException) ex).getCampo()).isEqualTo("password"));

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void registrar_conPasswordDe72BytesExactos_deberiaAceptarse() {
        when(repository.saveAndFlush(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        // 36 "ñ" = 72 bytes justos
        UsuarioResponse response = service.registrar(request("ana@mail.com", "ñ".repeat(36)));

        assertThat(response.email()).isEqualTo("ana@mail.com");
    }
}