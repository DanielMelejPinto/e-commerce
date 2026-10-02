package io.github.danielmelejpinto.pedidoapi.security;

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

    public JwtService(@Value("${seguridad.jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Token de corta duración para llamadas servicio a servicio (rol SYSTEM). */
    public String generarTokenSistema() {
        Date ahora = new Date();
        return Jwts.builder()
                .subject("pedido-api@interno")
                .claim("userId", 0L)
                .claim("rol", "SYSTEM")
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + 60_000))
                .signWith(secretKey)
                .compact();
    }

    public boolean validarToken(String token) {
        try {
            obtenerClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Long extraerUserId(String token) {
        return obtenerClaims(token).get("userId", Long.class);
    }

    private Claims obtenerClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
