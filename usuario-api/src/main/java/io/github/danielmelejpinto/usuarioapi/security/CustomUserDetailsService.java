package io.github.danielmelejpinto.usuarioapi.security;

import io.github.danielmelejpinto.usuarioapi.model.EstadoUsuario;
import io.github.danielmelejpinto.usuarioapi.model.Usuario;
import io.github.danielmelejpinto.usuarioapi.repository.UsuarioRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UsuarioRepository repositorioUsuarios;

    public CustomUserDetailsService(UsuarioRepository repositorioUsuarios) {
        this.repositorioUsuarios = repositorioUsuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String emailIngresado) throws UsernameNotFoundException {
        // Normalizamos el email antes de buscarlo
        String emailLimpio = Usuario.normalizarEmail(emailIngresado);
        
        Usuario usuarioBaseDatos = repositorioUsuarios.findByEmail(emailLimpio)
                .orElseThrow(() -> new UsernameNotFoundException("No se halló el usuario con email: " + emailLimpio));

        // 1. Validamos que el usuario no esté de baja
        if (usuarioBaseDatos.getEstado() != EstadoUsuario.ACTIVO) {
            throw new DisabledException("El usuario está inactivo o dado de baja.");
        }

        // 2. Retornamos el User que entiende Spring Security
        return new User(
                usuarioBaseDatos.getEmail(),
                usuarioBaseDatos.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + usuarioBaseDatos.getRol().name()))
        );
    }
}