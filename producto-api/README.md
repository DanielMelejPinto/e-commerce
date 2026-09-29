# producto-api

Microservicio REST desarrollado con **Java 21** y **Spring Boot 4** para la gestión de un catálogo de productos. Al crear un producto avisa a [`inventario-api`](../inventario-api) para que inicialice su inventario. Proyecto de práctica orientado a aplicar buenas prácticas de arquitectura, validación, manejo de errores, pruebas y documentación de APIs.

## Stack técnico

- **Java 21**
- **Spring Boot 4.1** (Web, Data JPA, Validation)
- **PostgreSQL 17** (persistencia en el perfil `docker`)
- **H2** (base de datos en memoria para desarrollo rápido y tests) + consola web en `dev`
- **springdoc-openapi** (documentación interactiva con Swagger UI)
- **Datafaker** (datos de prueba en `dev`)
- **JUnit 5 + Mockito + MockMvc** (tests de controller, service y manejo de errores)
- **Docker Compose** (para levantar PostgreSQL localmente)

## Características

- CRUD completo de productos (`crear`, `listar`, `obtener por id`, `actualizar`, `eliminar`)
- Paginación y ordenamiento configurables vía query params (con lista blanca de campos)
- Validación de datos de entrada con mensajes de error claros
- Control de concurrencia optimista (`@Version`) en las actualizaciones
- Integración con `inventario-api` con timeouts (2 s para conectar, 5 s para leer)
- Manejo centralizado de excepciones (`@RestControllerAdvice`), sin exponer detalles internos al cliente
- Documentación interactiva de la API con Swagger UI
- Tres perfiles: `dev` (H2 + datos de prueba), `docker` (PostgreSQL) y `test` (H2 vacío, solo para tests)

## Requisitos

- Java 21 (Maven no hace falta instalarlo: el proyecto incluye `mvnw`)
- Docker, solo si vas a usar el perfil `docker`
- `inventario-api` corriendo en el puerto 8081, solo para **crear** productos (ver [Integración con inventario-api](#integración-con-inventario-api))

## Cómo levantar el proyecto

### Opción 1 — Desarrollo rápido (H2 en memoria)

No requiere nada más instalado que Java 21. Los datos no persisten entre reinicios.

```bash
./mvnw spring-boot:run
```

La app queda disponible en `http://localhost:8080`. Por defecto usa el perfil `dev`, que además:

- carga **20 productos de prueba** con Datafaker (solo si la tabla está vacía),
- muestra el SQL de Hibernate en consola,
- habilita la consola de H2 en `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:productodb`, usuario `sa`, contraseña vacía).

> **Nota:** los productos de prueba se insertan directo en la base y no pasan por `POST /api/productos`, así que **no tienen inventario** en `inventario-api` (consultarlo da 404). Es lo esperado.

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

En este perfil la base arranca vacía (sin datos de prueba) y los datos persisten en el volumen `producto-data`, incluso si reinicias la app o borras el contenedor. Para apagar la base: `docker compose down` (con `-v` se borran también los datos).

## Configuración

| Propiedad / variable | Valor por defecto | Descripción |
|---|---|---|
| `server.port` | `8080` | Puerto de la API |
| `inventario.api.url` (env: `INVENTARIO_API_URL`) | `http://localhost:8081` | URL base de `inventario-api` |
| `inventario.api.connect-timeout-ms` | `2000` | Timeout de conexión a inventario-api (ms) |
| `inventario.api.read-timeout-ms` | `5000` | Timeout de lectura a inventario-api (ms) |
| `spring.data.web.pageable.max-page-size` | `50` | Tamaño máximo de página (si piden más, se recorta) |
| `POSTGRES_HOST` | `localhost` | Host de PostgreSQL (perfil `docker`) |
| `POSTGRES_DB` | `productodb` | Base de datos (perfil `docker`) |
| `POSTGRES_USER` | `producto` | Usuario (perfil `docker`) |
| `POSTGRES_PASSWORD` | *(obligatoria)* | Contraseña (perfil `docker`), se define en `.env` |

## Documentación de la API

Con la app corriendo, la documentación interactiva está disponible en:

```
http://localhost:8080/swagger-ui.html
```

## Endpoints

| Método   | Ruta                  | Descripción                             | Respuestas                  |
|----------|-----------------------|-----------------------------------------|-----------------------------|
| `POST`   | `/api/productos`      | Crear un producto                       | `201`, `400`                |
| `GET`    | `/api/productos`      | Listar productos (paginado y ordenable) | `200`, `400`                |
| `GET`    | `/api/productos/{id}` | Obtener un producto por id              | `200`, `400`, `404`         |
| `PUT`    | `/api/productos/{id}` | Actualizar un producto                  | `200`, `400`, `404`, `409`  |
| `DELETE` | `/api/productos/{id}` | Eliminar un producto                    | `204`, `404`                |

**Paginación y orden** (`GET /api/productos`): acepta `?page`, `?size` (máx. 50) y `?sort` (`id`, `nombre`, `precio`, `fechaCreacion`). Ejemplo: `?sort=precio,desc`.

**Validaciones** del producto: `nombre` obligatorio (máx. 150 caracteres) y `precio` obligatorio, mayor a cero, con hasta 10 enteros y 2 decimales.

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

## Integración con inventario-api (Patrón Outbox)

Al crear un producto, `producto-api` debe notificar a `inventario-api` para que inicialice el stock (POST a `/api/inventarios/producto/{id}`). Para evitar problemas de transacciones distribuidas y bloqueos si `inventario-api` está lento o caído, se implementó el **Patrón Outbox**:

1. Al crear el producto, se guarda en la base de datos local con estado `PENDIENTE` junto con un evento de creación en la tabla `outbox_events` (misma transacción local).
2. Un proceso en segundo plano (job planificado) lee periódicamente los eventos pendientes.
3. El proceso llama a `inventario-api`.
   - Si tiene éxito, marca el evento como `ENVIADO` y el producto cambia a estado `ACTIVO`.
   - Si falla temporalmente (error de red o 5xx), reintenta en la siguiente ejecución.
   - Si el inventario rechaza definitivamente la petición (4xx), el producto cambia a estado `BAJA`.

**Notas de negocio:**
- Un producto en estado `PENDIENTE` o `BAJA` **no** aparece en el listado (`GET /api/productos`).
- El borrado de un producto (`DELETE`) ahora es un **borrado lógico** (cambia a `BAJA`), para no romper la integridad referencial de futuros pedidos.

Los timeouts son configurables y evitan colapsar el sistema si la red está lenta.

## Manejo de errores

Todas las respuestas de error son JSON. Los errores de validación devuelven un mapa `campo → mensaje` (`400 Bad Request`):

```json
{
  "nombre": "El nombre es obligatorio",
  "precio": "El precio debe ser mayor a cero"
}
```

El resto usa la forma `{"error": "..."}`. Por ejemplo, al pedir un producto que no existe (`404 Not Found`):

```json
{
  "error": "Producto con id 99 no existe"
}
```

| Código | Cuándo ocurre |
|---|---|
| `400` | Datos inválidos, JSON mal formado, id no numérico o `sort` por un campo no permitido |
| `404` | El producto no existe |
| `409` | Dos operaciones modificaron el mismo producto a la vez (`@Version`); reintenta |
| `500` | Error inesperado. El detalle va al log del servidor, nunca al cliente |

## Correr los tests

```bash
./mvnw test
```

Los tests usan el perfil `test`: H2 en memoria y **vacía** (sin datos de prueba ni salida de SQL) y `inventario-api` simulado con Mockito. No necesitan Docker ni ninguna otra API levantada.

| Clase | Qué cubre | Tests |
|---|---|---|
| `ProductoControllerTest` | Endpoints, validaciones, paginación, orden y ciclo de vida de PENDIENTE a ACTIVO/BAJA | 30 |
| `ProductoServiceTest` | Lógica de negocio con repositorio simulado | 14 |
| `GlobalExceptionHandlerTest` | Respuesta `409` ante conflicto de concurrencia | 1 |
| `InventarioClientTest` | Timeout y mapeo de excepciones del RestClient | 4 |
| `OutboxProcessorTest` | Lógica de reintentos y actualización de estados | 4 |
| `DocumentacionApiTest` | Que el OpenAPI se genere y describa los endpoints | 1 |
| `ProductoApiApplicationTests` | Que el contexto de Spring arranque | 1 |

## Estructura del proyecto

```
src/main/java/io/github/danielmelejpinto/productoapi/
├── controller/     # Endpoints REST
├── service/        # Lógica de negocio
├── repository/     # Acceso a datos (Spring Data JPA)
├── model/          # Entidades JPA
├── dto/            # Objetos de entrada/salida (Request/Response)
├── exception/      # Excepciones personalizadas y manejo global
└── config/         # OpenAPI/Swagger, RestTemplate con timeouts y seeder de dev

src/main/resources/
├── application.properties          # Configuración común
├── application-dev.properties      # Perfil dev (H2, SQL visible, consola H2)
├── application-docker.properties   # Perfil docker (PostgreSQL)
└── META-INF/additional-spring-configuration-metadata.json   # Describe inventario.api.url para el IDE

src/test/resources/
└── application-test.properties     # Perfil test (H2 vacía)
```

## Limitaciones conocidas

- **Sin autenticación ni autorización.**
- Swagger UI y la consola de H2 están habilitados por defecto; el proyecto está pensado para desarrollo local, no para producción tal cual.

## Autor

**Daniel Melej Pinto**