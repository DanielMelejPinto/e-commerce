package io.github.danielmelejpinto.usuarioapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.core.Authentication;

import io.github.danielmelejpinto.usuarioapi.dto.LoginRequest;
import io.github.danielmelejpinto.usuarioapi.dto.RegistroRequest;
import io.github.danielmelejpinto.usuarioapi.dto.TokenResponse;
import io.github.danielmelejpinto.usuarioapi.dto.UsuarioResponse;
import io.github.danielmelejpinto.usuarioapi.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Registro y gestión de usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    // Sin header Location: el usuario no tiene una URL pública propia (solo /me)
    @PostMapping("/registro")
    @Operation(summary = "Registrar un usuario nuevo (siempre con rol USER)")
    @ApiResponse(responseCode = "201", description = "Usuario creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "409", description = "El email ya está registrado")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(request));
    }

    // NUEVO ENDPOINT
    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión (autenticación)")
    @ApiResponse(responseCode = "200", description = "Autenticación exitosa, retorna token JWT")
    @ApiResponse(responseCode = "401", description = "Credenciales incorrectas") // Lo mapea GlobalExceptionHandler (BadCredentialsException -> 401)
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(service.login(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener el perfil del usuario autenticado")
    @ApiResponse(responseCode = "200", description = "Perfil obtenido con éxito")
    @ApiResponse(responseCode = "401", description = "No autenticado o token inválido")
    public ResponseEntity<UsuarioResponse> obtenerMiPerfil(Authentication authentication) {
        String emailAutenticado = authentication.getName();
        return ResponseEntity.ok(service.obtenerPerfil(emailAutenticado));
    }
}