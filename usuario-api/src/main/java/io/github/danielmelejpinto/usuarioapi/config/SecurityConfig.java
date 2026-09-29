package io.github.danielmelejpinto.usuarioapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // TEMPORAL (paso 1): todo abierto. Se reemplaza en el paso 5.
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .headers(h -> h.frameOptions(f -> f.sameOrigin())) // para la consola H2
            .authorizeHttpRequests(a -> a.anyRequest().permitAll());
        return http.build();
    }
}