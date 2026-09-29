package io.github.danielmelejpinto.usuarioapi.service;

import java.nio.charset.StandardCharsets;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.danielmelejpinto.usuarioapi.dto.RegistroRequest;
import io.github.danielmelejpinto.usuarioapi.dto.UsuarioResponse;
import io.github.danielmelejpinto.usuarioapi.exception.CampoInvalidoException;
import io.github.danielmelejpinto.usuarioapi.exception.EmailYaRegistradoException;
import io.github.danielmelejpinto.usuarioapi.model.Usuario;
import io.github.danielmelejpinto.usuarioapi.repository.UsuarioRepository;

@Service
@Transactional(readOnly = true)
public class UsuarioService {

    // BCrypt solo usa los primeros 72 BYTES de la clave (una "ñ" ocupa 2)
    private static final int MAX_BYTES_PASSWORD = 72;

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_PASSWORD) {
            throw new CampoInvalidoException("password",
                    "La contraseña no puede superar 72 bytes (los caracteres con tilde o ñ ocupan 2)");
        }

        String email = Usuario.normalizarEmail(request.email());
        if (repository.existsByEmail(email)) {
            throw new EmailYaRegistradoException();
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(request.nombre().trim());
        usuario.setEmail(email);
        // La contraseña NO se recorta: los espacios también cuentan
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        // El rol nunca viene del cliente: todo registro público es USER (valor por defecto)

        try {
            return mapearAResponse(repository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultáneos con el mismo email: el segundo choca con el unique
            throw new EmailYaRegistradoException();
        }
    }

    private UsuarioResponse mapearAResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol(),
                usuario.getEstado(),
                usuario.getFechaCreacion());
    }
}