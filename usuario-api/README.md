# usuario-api

Microservicio de identidades del [e-commerce](../README.md): registro de usuarios, login y emisión de **JWT**. Construido con **Java 21**, **Spring Boot 4.1.1**, **Spring Security** y **jjwt 0.12.5**.

Otros módulos que se apoyan en él:

- [`pedido-api`](../pedido-api) valida los tokens emitidos aquí (mismo secreto JWT) y toma el `userId` del token.
- El [frontend](../frontend-app) lo usa para registro, login y perfil.

## Características

- **Registro** de usuarios: siempre con rol `USER`; el email se guarda en minúsculas y no puede repetirse.
- **Login** con email y contraseña; devuelve un JWT firmado (claim `userId`).
- **Perfil** del usuario autenticado (`/me`).
- Contraseñas con **BCrypt** (costo configurable) que nunca se devuelven ni se loguean.
- Sesiones **stateless**: cada petición protegida debe traer `Authorization: Bearer <token>`.
- Roles `USER` y `ADMIN` y estados `ACTIVO` y `BAJA` en el modelo.
- Documentación con OpenAPI y Swagger UI.

## Requisitos

- **JDK 21** (Maven no hace falta: el proyecto incluye `mvnw`).

## Cómo levantar el proyecto

```bash
./mvnw spring-boot:run
```

En PowerShell: `.\mvnw.cmd spring-boot:run`.

Arranca en el puerto **8082** con el perfil `dev` y H2 en memoria (los datos se pierden al reiniciar):

- API: `http://localhost:8082/api/usuarios`
- Swagger UI: `http://localhost:8082/swagger-ui.html`
- Consola H2: `http://localhost:8082/h2-console` (URL JDBC `jdbc:h2:mem:usuariodb`, usuario `sa`, contraseña vacía)

### Secreto JWT

El secreto se lee de la variable de entorno `JWT_SECRET`. Si no existe, se usa una clave de desarrollo incluida en `application.properties` (**pública: no la uses fuera de local**). Debe tener al menos 32 caracteres.

```bash
export JWT_SECRET='pon_aqui_un_secreto_largo_de_al_menos_32_caracteres'
./mvnw spring-boot:run
```

`pedido-api` necesita **el mismo valor** (ver su [README](../pedido-api/README.md)).

## Configuración

| Propiedad / variable | Predeterminado | Descripción |
|---|---|---|
| `server.port` | `8082` | Puerto HTTP |
| `seguridad.jwt.secret` (`JWT_SECRET`) | Clave de desarrollo | Secreto de firma del JWT |
| `seguridad.jwt.expiration-ms` | `86400000` | Vigencia del token (1 día) |
| `seguridad.bcrypt-cost` | `12` | Costo de BCrypt (cada +1 duplica el tiempo) |
| `spring.profiles.default` | `dev` | Perfil `dev`: SQL visible y consola H2 |

## Endpoints

| Método | Ruta | Acceso | Descripción | Respuestas |
|---|---|---|---|---|
| `POST` | `/api/usuarios/registro` | Público | Registrar un usuario nuevo (rol `USER`) | `201`, `400`, `409` |
| `POST` | `/api/usuarios/login` | Público | Iniciar sesión; devuelve `{"token": "..."}` | `200`, `400`, `401` |
| `GET` | `/api/usuarios/me` | JWT | Perfil del usuario autenticado | `200`, `403` |

Públicos además: Swagger UI (`/swagger-ui.html`, `/v3/api-docs/**`) y `/h2-console/**`. Todo lo demás exige JWT.

**Validaciones del registro:** `nombre` obligatorio (máx. 150 caracteres); `email` obligatorio, con formato válido (máx. 254); `password` obligatoria, de 8 a 72 caracteres.

### Ejemplo de uso

```bash
# Registro
curl -X POST http://localhost:8082/api/usuarios/registro \
  -H "Content-Type: application/json" \
  -d '{"nombre":"Ana Pérez","email":"ana@mail.com","password":"ClaveSegura1"}'
```

Respuesta (`201 Created`, sin la contraseña):

```json
{
  "id": 1,
  "nombre": "Ana Pérez",
  "email": "ana@mail.com",
  "rol": "USER",
  "estado": "ACTIVO",
  "fechaCreacion": "2026-09-29T10:00:00"
}
```

```bash
# Login
curl -X POST http://localhost:8082/api/usuarios/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana@mail.com","password":"ClaveSegura1"}'
# → {"token":"eyJhbGciOi..."}

# Perfil (con el token recibido)
curl http://localhost:8082/api/usuarios/me -H "Authorization: Bearer $TOKEN"
```

## Manejo de errores

Los errores de validación devuelven `400` con un mapa `campo → mensaje`; el resto usa `{"error": "..."}`.

| Código | Cuándo ocurre |
|---|---|
| `400` | Datos inválidos, JSON mal formado o contraseña fuera de los límites |
| `401` | Credenciales incorrectas en el login |
| `403` | `/me` sin token o con token inválido |
| `409` | El email ya está registrado |
| `500` | Error inesperado; el detalle va al log del servidor |

## Roles y administradores

El registro **siempre crea usuarios `USER`** y no existe un endpoint para asignar `ADMIN`. Para probar el panel de administración del frontend, cambia el rol del usuario en la base (por ejemplo desde la consola H2) y vuelve a iniciar sesión. Por ahora el backend **no restringe endpoints por rol**: el rol solo se devuelve en el perfil y el frontend lo usa para mostrar `/admin`.

## Correr los tests

```bash
./mvnw test
```

Cubren el modelo (`UsuarioTest`), el repositorio (`UsuarioRepositoryTest`), el servicio (`UsuarioServiceTest`), el controlador con MockMvc (`UsuarioControllerTest`) y el arranque del contexto (`UsuarioApiApplicationTests`). No requieren Docker.

## Estructura del proyecto

```
src/main/java/io/github/danielmelejpinto/usuarioapi/
├── config/         # SecurityConfig (filtros, BCrypt) y OpenAPI
├── controller/     # UsuarioController
├── dto/            # RegistroRequest, LoginRequest, TokenResponse, UsuarioResponse
├── exception/      # Excepciones y manejo global
├── model/          # Usuario, Rol, EstadoUsuario
├── repository/     # UsuarioRepository (Spring Data JPA)
├── security/       # JwtService, JwtAuthenticationFilter, CustomUserDetailsService
└── service/        # UsuarioService

src/main/resources/
├── application.properties       # Puerto, JWT, BCrypt
└── application-dev.properties   # SQL visible y consola H2
```

## Limitaciones conocidas

- **Bases en memoria (H2):** los usuarios se pierden al reiniciar. El driver de PostgreSQL está en el `pom.xml`, pero no hay perfil ni configuración para usarlo.
- **Clave JWT por defecto pública** y secreto compartido con `pedido-api` por configuración manual.
- **Sin autorización por rol** en el backend, sin alta de administradores por API y sin `logout` ni revocación de tokens (un token es válido hasta que expira).
- **Sin refresh tokens** ni recuperación de contraseña.
- Swagger UI y consola H2 públicos: pensado para desarrollo local.

## Autor

**Daniel Melej Pinto** · [GitHub](https://github.com/DanielMelejPinto)