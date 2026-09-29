package io.github.danielmelejpinto.usuarioapi.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import io.github.danielmelejpinto.usuarioapi.model.EstadoUsuario;
import io.github.danielmelejpinto.usuarioapi.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    // Quien llame debe pasar el email ya normalizado (minúsculas y sin espacios)
    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<Usuario> findByEstado(EstadoUsuario estado, Pageable pageable);
}