package io.github.danielmelejpinto.usuarioapi.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Buscamos el header "Authorization"
        final String authHeader = request.getHeader("Authorization");
        
        // 2. Si no hay header o no empieza con "Bearer ", ignoramos y pasamos al siguiente filtro
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraemos el token (cortando los primeros 7 caracteres: "Bearer ")
        final String jwt = authHeader.substring(7);
        
        // 4. Validamos el token
        if (jwtService.validarToken(jwt)) {
            String email = jwtService.extraerEmail(jwt);

            // 5. Si obtuvimos el email y el usuario aún no está autenticado en este hilo
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Cargamos los datos del usuario desde la base de datos (con nuestro CustomUserDetailsService)
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(email);

                // 6. Creamos el objeto de autenticación que usa Spring
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // 7. Establecemos al usuario como "Logueado" para el resto de la petición
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // 8. Continuamos la cadena de filtros
        filterChain.doFilter(request, response);
    }
}