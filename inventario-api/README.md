# inventario-api

Microservicio de inventario del [e-commerce](../README.md), con **Java 21** y **Spring Boot 4.1.1**.

## Descripción

Gestiona las existencias y reservas de stock por producto. Es un servicio **interno** que consumen:

- [`producto-api`](../producto-api): inicializa el inventario al crear un producto (vía Outbox).
- [`pedido-api`](../pedido-api): reserva stock al crear un pedido y lo libera si el pedido falla.
- El [frontend](../frontend-app): el panel `/admin` consulta e inicializa inventarios y agrega stock.

## Stack tecnológico

- **Java 21** y **Spring Boot 4.1.1** (Web MVC, Data JPA, Validation, Actuator)
- **H2** en memoria como base de ejecución (ver [Limitaciones conocidas](#limitaciones-conocidas))
- **springdoc-openapi** (Swagger UI)
- **JUnit 5 + Mockito + MockMvc**
- **Testcontainers + PostgreSQL 17** (pruebas de integración)
- **Maven** vía `mvnw`

> Flyway y el driver de PostgreSQL están en el `pom.xml` como preparación para migrar a PostgreSQL, pero hoy Flyway está desactivado (`spring.flyway.enabled=false`) y no hay scripts de migración.

## Requisitos

- Java 21 (no hace falta instalar Maven: el proyecto incluye `mvnw`).
- Docker, para ejecutar las pruebas de integración con Testcontainers (si no está, esas pruebas se omiten).

## Cómo levantar el proyecto

```bash
./mvnw spring-boot:run
```

Arranca en el puerto **8081** con H2 en memoria:

- API: `http://localhost:8081`
- Swagger UI: `http://localhost:8081/swagger-ui.html`
- Salud: `http://localhost:8081/actuator/health`

> **Importante:** los datos viven en memoria y **se pierden al reiniciar**. Si `producto-api` conserva sus productos y reinicias este servicio, esos productos quedarán sin inventario (`404`) hasta que se inicialice de nuevo con el `POST`.

## Endpoints

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| `POST` | `/api/inventarios/producto/{productoId}` | Inicializa stock en 0 para un producto | `201` (creado), `200` (ya existía) |
| `GET` | `/api/inventarios/producto/{productoId}` | Consulta stock disponible y reservado | `200`, `404` |
| `PUT` | `/api/inventarios/producto/{productoId}/agregar` | Suma unidades al stock disponible | `200`, `400`, `404`, `409` |
| `PUT` | `/api/inventarios/producto/{productoId}/reservar` | Mueve stock de disponible a reservado | `200`, `400`, `404`, `409` |
| `PUT` | `/api/inventarios/producto/{productoId}/liberar` | Devuelve unidades reservadas a disponible (compensación de pedidos) | `200` |
| `DELETE` | `/api/inventarios/producto/{productoId}` | Elimina el inventario de un producto | `204` |

**Validaciones** de `agregar`, `reservar` y `liberar`: `cantidad` obligatoria, mayor a cero y como máximo `100000` por operación. Un id no numérico produce `400`.

### Detalles de comportamiento

- **`POST` idempotente:** si el inventario ya existe, devuelve el existente **sin modificarlo** y responde `200`; si lo crea, responde `201`.
- **`DELETE` idempotente:** responde `204` exista o no el inventario.
- **`reservar`:** mueve unidades de `cantidadDisponible` a `cantidadReservada` con una actualización condicional atómica. Si no alcanza el stock, responde `409`.
- **`liberar`:** operación inversa de `reservar`, usada por `pedido-api` cuando debe deshacer reservas.
- Repetir `agregar`, `reservar` o `liberar` vuelve a aplicar la operación (no hay clave de idempotencia).

### Ejemplo — agregar stock

```bash
curl -X PUT http://localhost:8081/api/inventarios/producto/1/agregar \
  -H "Content-Type: application/json" \
  -d '{"cantidad": 10}'
```

Respuesta (`200 OK`):

```json
{
  "productoId": 1,
  "cantidadDisponible": 10,
  "cantidadReservada": 0,
  "ultimaActualizacion": "2026-09-29T10:15:00"
}
```

## Manejo de errores

Los errores de validación devuelven un mapa `campo → mensaje`; el resto usa `{"error": "..."}`.

```json
{ "cantidad": "La cantidad debe ser mayor a cero" }
```

| Código | Cuándo ocurre |
|---|---|
| `400` | Datos inválidos, JSON mal formado, id no numérico o cantidad fuera de límite |
| `404` | El producto no tiene inventario |
| `409` | Stock insuficiente, conflicto de concurrencia (`@Version`) o violación de restricción (por ejemplo `productoId` duplicado) |
| `500` | Error inesperado; el detalle va al log del servidor |

> Un `409` por concurrencia **no significa necesariamente que falte stock**: el cliente puede reintentar.

## Correr los tests

```bash
./mvnw test
```

- `InventarioControllerTest` e `InventarioApiApplicationTests` usan **PostgreSQL 17 con Testcontainers** (contenedor compartido) y `@Testcontainers(disabledWithoutDocker = true)`: **sin Docker se omiten**, y un resultado exitoso no confirma esa integración. Revisa la salida de Maven o `target/surefire-reports/`.
- `DocumentacionApiTest` usa H2 en memoria.
- `InventarioServiceTest` y `GlobalExceptionHandlerTest` son pruebas unitarias sin contenedores.

## Estructura del proyecto

```
src/main/java/io/github/danielmelejpinto/inventarioapi/
├── controller/     # Endpoints REST
├── service/        # Lógica de negocio
├── repository/     # Acceso a datos (Spring Data JPA)
├── model/          # Entidades JPA
├── dto/            # Objetos de entrada/salida
└── exception/      # Excepciones y manejo global

src/main/resources/
└── application.properties          # Puerto 8081, H2, Flyway desactivado

src/test/java/.../inventarioapi/
├── controller/     # Pruebas de endpoints (MockMvc)
├── service/        # Pruebas unitarias con Mockito
├── exception/      # Pruebas del manejador global de errores
└── TestcontainersConfiguration.java   # PostgreSQL 17 para pruebas de integración

src/test/resources/
└── application-test.properties     # Perfil test
```

## Relación con otros servicios

- **producto-api:** al crear un producto (`PENDIENTE`), un proceso Outbox llama a `POST /api/inventarios/producto/{id}`; si tiene éxito, el producto pasa a `ACTIVO`. Los productos de ejemplo del perfil `dev` no pasan por este flujo y no tienen inventario.
- **pedido-api:** reserva con `PUT .../reservar` por cada ítem y, si el pedido falla, llama a `PUT .../liberar`.

## Limitaciones conocidas

- **Sin autenticación ni autorización.** Cualquiera con acceso al puerto 8081 puede agregar, reservar, liberar o borrar stock; no debería exponerse fuera de la red interna.
- **Datos volátiles:** solo usa H2 en memoria. Si `producto-api` reinicia su base y reutiliza ids, `POST` devolverá el inventario antiguo con su stock en lugar de uno nuevo.
- **Reservas sin identificador:** no existe la reserva como entidad; no se puede confirmar tras un pago ni caducar. Reintentar `reservar` tras un timeout **reserva dos veces**.
- **Conflictos de concurrencia sin reintento interno:** dos operaciones simultáneas sobre el mismo inventario pueden dar `409` aunque haya stock; el cliente debe reintentar.
- **Eliminación insegura:** `DELETE` borra el inventario aunque tenga reservas activas.
- **Sin migraciones:** el esquema lo genera Hibernate (`ddl-auto=update`); Flyway está preparado pero desactivado.

## Autor

**Daniel Melej Pinto** · [GitHub](https://github.com/DanielMelejPinto)