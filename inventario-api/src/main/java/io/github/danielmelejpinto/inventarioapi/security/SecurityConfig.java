package io.github.danielmelejpinto.inventarioapi.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Value("${spring.h2.console.enabled:false}")
    private boolean h2ConsoleEnabled;

    @Value("${springdoc.api-docs.enabled:true}")
    private boolean apiDocsEnabled;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/actuator/**", "/error").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/inventarios/producto/**").permitAll();
                    
                if (apiDocsEnabled) {
                    auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
                }
                
                if (h2ConsoleEnabled) {
                    auth.requestMatchers("/h2-console/**").permitAll();
                }
                
                auth.requestMatchers(HttpMethod.POST, "/api/inventarios/producto/**").hasAnyRole("ADMIN", "SYSTEM")
                    .requestMatchers(HttpMethod.DELETE, "/api/inventarios/producto/**").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/inventarios/producto/*/agregar").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.PUT, "/api/inventarios/producto/*/reservar", "/api/inventarios/producto/*/liberar").hasAnyRole("ADMIN", "SYSTEM")
                    .anyRequest().authenticated();
            })
            .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
    @Bean
    public UserDetailsService dummyUserDetailsService() {
        return username -> { throw new UsernameNotFoundException("No users in this service"); };
    }
}
