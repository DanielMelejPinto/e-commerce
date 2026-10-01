package io.github.danielmelejpinto.usuarioapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import io.github.danielmelejpinto.usuarioapi.security.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Value("${springdoc.api-docs.enabled:true}")
    private boolean apiDocsEnabled;

    // Inyectamos el filtro que acabamos de crear
    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .headers(h -> h.frameOptions(f -> f.sameOrigin())) // Para la consola H2
            // JWT no usa sesión, le indicamos a Spring que sea STATELESS
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/actuator/**", "/error").permitAll()
                    // Endpoints públicos: registro y login
                    .requestMatchers("/api/usuarios/registro", "/api/usuarios/login").permitAll();
                
                if (apiDocsEnabled) {
                    // Swagger para la documentación
                    auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                }
                
                if (h2ConsoleEnabled) {
                    // Consola H2
                    auth.requestMatchers("/h2-console/**").permitAll();
                }
                
                // Cualquier otra petición exigirá estar autenticado (tener JWT)
                auth.anyRequest().authenticated();
            })
            // Sin autenticar -> 401 (por defecto Spring respondería 403)
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            // Ejecutar nuestro filtro ANTES del filtro oficial de Spring Security
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder(@Value("${seguridad.bcrypt-cost:12}") int cost) {
        return new BCryptPasswordEncoder(cost);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}
