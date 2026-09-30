package io.github.danielmelejpinto.usuarioapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long jwtExpirationMs;

    public JwtService(@Value("${seguridad.jwt.secret}") String secret,
                      @Value("${seguridad.jwt.expiration-ms:86400000}") long jwtExpirationMs) {
        // Genera la clave criptográfica usando la propiedad configurada
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public String generarToken(String email, Long usuarioId) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(email) // El "subject" del token será el email del usuario
                .claim("userId", usuarioId)
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(secretKey)
                .compact();
    }

    public String extraerEmail(String token) {
        return obtenerClaims(token).getSubject();
    }

    public Long extraerUserId(String token) {
        return obtenerClaims(token).get("userId", Long.class);
    }

    public boolean validarToken(String token) {
        try {
            obtenerClaims(token);
            return true;
        } catch (Exception e) {
            // El token expiró, la firma es inválida, o está mal formado
            return false;
        }
    }

    private Claims obtenerClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token) // Si el token es inválido, esto lanza una excepción
                .getPayload();
    }
}