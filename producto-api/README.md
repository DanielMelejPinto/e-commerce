# producto-api

Microservicio REST de catálogo de productos, con **Java 21** y **Spring Boot 4.1.1**. Al crear un producto, [`inventario-api`](../inventario-api) inicializa su inventario mediante el patrón **Transactional Outbox**. Forma parte del [e-commerce](../README.md) y es consumido por [`pedido-api`](../pedido-api) (precio actual) y por el [frontend](../frontend-app).

## Stack técnico

- **Java 21** y **Spring Boot 4.1.1** (Web MVC, Data JPA, Validation)
- **PostgreSQL 17** (perfil `docker`) y **H2** en memoria (desarrollo y pruebas)
- **springdoc-openapi** (Swagger UI) y **Datafaker** (datos de ejemplo en `dev`)
- **JUnit 5 + Mockito + MockMvc** (pruebas)
- **Docker Compose** (PostgreSQL local)

## Características

- Crear, listar, consultar, actualizar y dar de baja productos (`nombre`, `precio`, `descripcion` e `imagenUrl` opcionales).
- Estados `PENDIENTE`, `ACTIVO` y `BAJA`; el `DELETE` es un **borrado lógico** (pasa a `BAJA`).
- Paginación y orden con lista blanca de campos.
- Validación de entrada y manejo centralizado de errores en JSON (`@RestControllerAdvice`), sin exponer detalles internos.
- Control de concurrencia optimista (`@Version`).
- Integración con inventario mediante Outbox y **Apache Kafka**, garantizando la entrega de eventos con soporte para reintentos y tolerancia a fallos.
- Tres perfiles: `dev` (H2 + datos de ejemplo + consola H2), `docker` (PostgreSQL) y `test` (H2 vacía).

## Requisitos

- Java 21 (Maven no hace falta: el proyecto incluye `mvnw`).
- Docker, solo para el perfil `docker`.
- [`inventario-api`](../inventario-api) en el puerto 8081 para que los productos **creados** pasen de `PENDIENTE` a `ACTIVO`.

## Cómo levantar el proyecto

### Opción 1 — Desarrollo rápido (H2 en memoria)

```bash
./mvnw spring-boot:run
```

Queda en `http://localhost:8080` con el perfil `dev`, que además:

- inserta **20 productos de ejemplo** si la tabla está vacía,
- muestra el SQL de Hibernate,
- habilita la consola H2 en `http://localhost:8080/h2-console` (URL `jdbc:h2:mem:productodb`, usuario `sa`, contraseña vacía).

> Los productos de ejemplo se insertan directo en la base: **no pasan por el Outbox ni tienen inventario** (consultarlo da `404`). Es lo esperado.

### Opción 2 — Con PostgreSQL (perfil `docker`)

1. Copia `.env.example` a `.env` y cambia `POSTGRES_PASSWORD`:

   ```bash
   cp .env.example .env
   ```

2. Levanta la base de datos y comprueba que esté saludable:

   ```bash
   docker compose up -d
   docker compose ps
   ```

3. Inicia la aplicación exportando las variables del `.env`:

   ```bash
   set -a
   source ./.env
   set +a
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=docker
   ```

   Si la contraseña tiene caracteres especiales, escríbela entre comillas simples en el `.env`.

En este perfil la base parte vacía (sin datos de ejemplo) y los datos persisten en el volumen `producto-data`. Para apagar: `docker compose down` (con `-v` se borran también los datos).

## Configuración

| Propiedad / variable | Predeterminado | Descripción |
|---|---|---|
| `server.port` | `8080` | Puerto de la API |
| `inventario.api.url` (`INVENTARIO_API_URL`) | `http://localhost:8081` | URL base de `inventario-api` |
| `inventario.api.connect-timeout-ms` | `2000` | Timeout de conexión (ms) |
| `inventario.api.read-timeout-ms` | `5000` | Timeout de lectura (ms) |
| `outbox.max-intentos` | `5` | Reintentos máximos por evento |
| `spring.data.web.pageable.max-page-size` | `50` | Tamaño máximo de página (si piden más, se recorta) |
| `POSTGRES_HOST` | `localhost` | Host de PostgreSQL (perfil `docker`) |
| `POSTGRES_DB` | `productodb` | Base de datos (perfil `docker`) |
| `POSTGRES_USER` | `producto` | Usuario (perfil `docker`) |
| `POSTGRES_PASSWORD` | *(obligatoria)* | Contraseña (perfil `docker`), definida en `.env` |

El intervalo del procesador Outbox (5000 ms) está fijo en el código.

## Documentación de la API

Con la app corriendo: `http://localhost:8080/swagger-ui.html` (OpenAPI en `/v3/api-docs`).

## Endpoints

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| `POST` | `/api/productos` | Crear un producto (queda `PENDIENTE`) | `201`, `400` |
| `GET` | `/api/productos` | Listar productos activos (paginado y ordenable) | `200`, `400` |
| `GET` | `/api/productos/{id}` | Obtener un producto `PENDIENTE` o `ACTIVO` | `200`, `400`, `404` |
| `PUT` | `/api/productos/{id}` | Actualizar un producto | `200`, `400`, `404`, `409` |
| `DELETE` | `/api/productos/{id}` | Dar de baja (borrado lógico) | `204`, `400`, `404`, `409` |

**Paginación y orden** (`GET /api/productos`): `?page` (desde 0), `?size` (por defecto 10, máx. 50) y `?sort` (`id`, `nombre`, `precio`, `fechaCreacion`). Ejemplo: `?sort=precio,desc`.

**Validaciones:** `nombre` obligatorio (máx. 150 caracteres); `precio` obligatorio, mayor a cero, hasta 17 enteros y 2 decimales; `descripcion` e `imagenUrl` opcionales.

### Ejemplo — crear un producto

```bash
curl -X POST http://localhost:8080/api/productos \
  -H "Content-Type: application/json" \
  -d '{"nombre": "Teclado mecánico", "precio": 49.90, "descripcion": "Teclado RGB", "imagenUrl": "https://ejemplo.com/teclado.jpg"}'
```

Respuesta (`201 Created`, con header `Location` hacia el nuevo recurso):

```json
{
  "id": 1,
  "nombre": "Teclado mecánico",
  "precio": 49.90,
  "descripcion": "Teclado RGB",
  "imagenUrl": "https://ejemplo.com/teclado.jpg",
  "fechaCreacion": "2026-09-28T02:15:00",
  "estado": "PENDIENTE"
}
```

## Integración con inventario-api (Outbox)

Al crear un producto, `producto-api` debe pedir a `inventario-api` que inicialice el stock (`POST /api/inventarios/producto/{id}`). Para no depender de una transacción distribuida ni bloquear al cliente si inventario está lento o caído:

1. Se guarda el producto (`PENDIENTE`) y un evento de creación en `outbox_events`, en la misma transacción local.
2. Un proceso planificado (`@Scheduled(fixedDelay = 5000)`) lee los eventos pendientes.
3. Llama a `inventario-api` y actualiza los estados:

| Resultado | Producto | Evento |
|---|---|---|
| Éxito | `ACTIVO` | `ENVIADO` |
| Rechazo `4xx` | `BAJA` | `ERROR` |
| Error de red, timeout o `5xx` | Sin cambios | Pendiente, aumenta `intentos` |
| Producto dado de baja antes de procesarse | `BAJA` | `ENVIADO`, sin llamar a inventario |

**Notas de negocio:**

- Un producto `PENDIENTE` o `BAJA` **no** aparece en el listado.
- Un producto `BAJA` responde `404` en consulta y actualización por ID.
- La baja **no elimina el inventario**: el `DELETE` de inventario es independiente.
- Consistencia eventual: un `201` no garantiza que el inventario ya exista.

## Manejo de errores

Los errores de validación devuelven `400` con un mapa `campo → mensaje`; el resto usa `{"error": "..."}`.

```json
{ "nombre": "El nombre es obligatorio", "precio": "El precio debe ser mayor a cero" }
```

```json
{ "error": "Producto con id 99 no existe" }
```

| Código | Cuándo ocurre |
|---|---|
| `400` | Datos inválidos, JSON mal formado, id no numérico o `sort` por un campo no permitido |
| `404` | El producto no existe o está dado de baja |
| `409` | Conflicto de concurrencia (`@Version`); reintenta |
| `500` | Error inesperado; el detalle va al log del servidor |

## Correr los tests

```bash
./mvnw test
```

Usan el perfil `test`: H2 vacía y `inventario-api` simulado con Mockito. **No necesitan Docker ni otra API levantada.**

| Clase | Qué cubre |
|---|---|
| `ProductoControllerTest` | Endpoints, validaciones, paginación, orden y ciclo `PENDIENTE` → `ACTIVO`/`BAJA` |
| `ProductoServiceTest` | Lógica de negocio con repositorio simulado |
| `GlobalExceptionHandlerTest` | Respuestas ante conflictos de concurrencia |
| `InventarioClientTest` | Timeouts y mapeo de excepciones del cliente HTTP |
| `OutboxProcessorTest` | Reintentos y actualización de estados |
| `DocumentacionApiTest` | Generación del OpenAPI |
| `ProductoApiApplicationTests` | Arranque del contexto de Spring |

## Estructura del proyecto

```
src/main/java/io/github/danielmelejpinto/productoapi/
├── client/         # Cliente HTTP hacia inventario-api
├── config/         # OpenAPI, RestClient con timeouts y seeder de dev
├── controller/     # Endpoints REST
├── dto/            # Objetos de entrada/salida
├── exception/      # Excepciones y manejo global
├── model/          # Entidades JPA (Producto, OutboxEvent) y enums de estado
├── repository/     # Acceso a datos (Spring Data JPA)
└── service/        # Lógica de negocio y OutboxProcessor

src/main/resources/
├── application.properties          # Configuración común
├── application-dev.properties      # Perfil dev (SQL visible, consola H2)
├── application-docker.properties   # Perfil docker (PostgreSQL)
└── META-INF/additional-spring-configuration-metadata.json

src/test/resources/
└── application-test.properties     # Perfil test (H2 vacía)
```

## Limitaciones conocidas

- **Sin autenticación ni autorización:** cualquiera con acceso al puerto 8080 puede crear, modificar o dar de baja productos.
- Swagger UI y la consola H2 están habilitados por defecto: pensado para desarrollo local.
- El Outbox procesa eventos de forma secuencial y sin espera progresiva; no está preparado para varias instancias.
- Los productos `dev` de ejemplo no tienen inventario.

## Autor

**Daniel Melej Pinto** · [GitHub](https://github.com/DanielMelejPinto)