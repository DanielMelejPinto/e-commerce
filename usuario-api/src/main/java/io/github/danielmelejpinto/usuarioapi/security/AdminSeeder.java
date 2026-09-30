package io.github.danielmelejpinto.usuarioapi.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import io.github.danielmelejpinto.usuarioapi.model.Rol;
import io.github.danielmelejpinto.usuarioapi.model.Usuario;
import io.github.danielmelejpinto.usuarioapi.repository.UsuarioRepository;

@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email:#{null}}")
    private String adminEmail;

    @Value("${admin.password:#{null}}")
    private String adminPassword;

    public AdminSeeder(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (adminEmail == null || adminEmail.isBlank() || adminPassword == null || adminPassword.isBlank()) {
            log.info("No se ha configurado ADMIN_EMAIL y ADMIN_PASSWORD. Se omite la creación del administrador inicial.");
            return;
        }

        String emailLimpio = Usuario.normalizarEmail(adminEmail);
        
        if (usuarioRepository.existsByEmail(emailLimpio)) {
            log.info("El administrador principal ({}) ya existe en la base de datos.", emailLimpio);
            return;
        }

        Usuario admin = new Usuario();
        admin.setEmail(emailLimpio);
        admin.setNombre("Administrador");
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRol(Rol.ADMIN);

        usuarioRepository.save(admin);
        log.info("Se ha creado exitosamente el usuario administrador inicial: {}", emailLimpio);
    }
}
