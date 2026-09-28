# producto-api

Microservicio REST desarrollado con **Java 21** y **Spring Boot 4** para la gestión de un catálogo de productos. Proyecto de práctica orientado a aplicar buenas prácticas de arquitectura, validación, manejo de errores y documentación de APIs.

## Stack técnico

- **Java 21**
- **Spring Boot 4.1** (Web, Data JPA, Validation)
- **PostgreSQL** (persistencia en ambiente `docker`)
- **H2** (base de datos en memoria para desarrollo rápido y tests)
- **springdoc-openapi** (documentación interactiva con Swagger UI)
- **JUnit 5 + MockMvc** (tests de controller y service)
- **Docker Compose** (para levantar PostgreSQL localmente)

## Características

- CRUD completo de productos (`crear`, `listar`, `obtener por id`, `actualizar`, `eliminar`)
- Paginación y ordenamiento configurables vía query params
- Validación de datos de entrada con mensajes de error claros
- Manejo centralizado de excepciones (`@RestControllerAdvice`), sin exponer detalles internos al cliente
- Documentación interactiva de la API con Swagger UI
- Dos perfiles de ejecución: `dev` (H2 en memoria) y `docker` (PostgreSQL)
- Tests unitarios y de integración para controller y service

## Cómo levantar el proyecto

### Opción 1 — Desarrollo rápido (H2 en memoria)

No requiere nada más instalado que Java 21 y Maven. Los datos no persisten entre reinicios.

```bash
./mvnw spring-boot:run
```

La app queda disponible en `http://localhost:8080`. Por defecto usa el perfil `dev`.

### Opción 2 — Con PostgreSQL (perfil `docker`)

1. Copia `.env.example` a `.env` y define tus propias credenciales:

   ```bash
   cp .env.example .env
   ```

2. Levanta la base de datos:

   ```bash
   docker compose up -d
   ```

3. Corre la aplicación con el perfil `docker`, exportando las mismas variables del `.env`:

   ```bash
   export $(cat .env | xargs) && ./mvnw spring-boot:run -Dspring-boot.run.profiles=docker
   ```

## Documentación de la API

Con la app corriendo, la documentación interactiva está disponible en:

```
http://localhost:8080/swagger-ui.html
```

## Endpoints

| Método   | Ruta                  | Descripción                                   |
|----------|-----------------------|------------------------------------------------|
| `POST`   | `/api/productos`      | Crear un producto                              |
| `GET`    | `/api/productos`      | Listar productos (paginado y ordenable)        |
| `GET`    | `/api/productos/{id}` | Obtener un producto por id                     |
| `PUT`    | `/api/productos/{id}` | Actualizar un producto                         |
| `DELETE` | `/api/productos/{id}` | Eliminar un producto                           |

**Paginación y orden** (`GET /api/productos`): acepta `?page`, `?size` (máx. 50) y `?sort` (`id`, `nombre`, `precio`, `fechaCreacion`). Ejemplo: `?sort=precio,desc`.

### Ejemplo — crear un producto

```bash
curl -X POST http://localhost:8080/api/productos \
  -H "Content-Type: application/json" \
  -d '{"nombre": "Teclado mecánico", "precio": 49.90}'
```

Respuesta (`201 Created`, con header `Location` apuntando al nuevo recurso):

```json
{
  "id": 1,
  "nombre": "Teclado mecánico",
  "precio": 49.90,
  "fechaCreacion": "2026-09-28T02:15:00"
}
```

### Manejo de errores

Todas las respuestas de error siguen un formato JSON consistente. Por ejemplo, al enviar datos inválidos (`400 Bad Request`):

```json
{
  "nombre": "El nombre es obligatorio",
  "precio": "El precio debe ser mayor a cero"
}
```

O al pedir un producto que no existe (`404 Not Found`):

```json
{
  "error": "Producto con id 99 no encontrado"
}
```

## Correr los tests

```bash
./mvnw test
```

## Estructura del proyecto

```
src/main/java/io/github/danielmelejpinto/productoapi/
├── controller/     # Endpoints REST
├── service/        # Lógica de negocio
├── repository/     # Acceso a datos (Spring Data JPA)
├── model/          # Entidades JPA
├── dto/            # Objetos de entrada/salida (Request/Response)
├── exception/      # Excepciones personalizadas y manejo global
└── config/         # Configuración (OpenAPI/Swagger)
```

## Próximos pasos

- [ ] Integración continua con GitHub Actions
- [ ] Dockerfile para empaquetar la aplicación
- [ ] Despliegue público (demo en vivo)
- [ ] Reporte de cobertura de tests

## Autor

**Daniel Melej Pinto**
