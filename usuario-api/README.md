# E-Commerce · Servicio de Usuarios (`usuario-api`)

Microservicio encargado de la gestión de identidades, registro de usuarios, autenticación y autorización para el sistema de e-commerce.

Este servicio está construido con **Java 21** y **Spring Boot 4.1.1**, utilizando **Spring Security** y **JSON Web Tokens (JWT)** para proteger los accesos al sistema.

## Características principales

- **Gestión de Identidades**: Registro y administración de usuarios.
- **Autenticación con JWT**: Generación y validación de tokens seguros (`jjwt` 0.12.5) para el control de sesiones sin estado (stateless).
- **Autorización (Spring Security)**: Protección de rutas y control de acceso basado en roles o permisos.
- **Persistencia**: Compatible con bases de datos PostgreSQL (producción) y H2 en memoria (desarrollo local/pruebas) a través de Spring Data JPA.
- **Documentación Interactiva**: Integración automática con OpenAPI y Swagger UI.

## Ejecución Local

### Requisitos
- **JDK 21**
- (Opcional) Docker si utilizas una base de datos PostgreSQL localmente.

### Iniciar el servicio

Ubicado en la raíz de la carpeta `usuario-api`, ejecuta:

```bash
./mvnw spring-boot:run