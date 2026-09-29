# Inventario API

Microservicio de inventario del e-commerce, desarrollado con **Java 21** y **Spring Boot 4**.

## Descripción

Gestiona las existencias y reservas de stock por producto, trabajando de la mano con [`producto-api`](../producto-api). Es un servicio **interno**: lo consume `producto-api` (inicializar y eliminar inventarios) y, en el futuro, el servicio que gestione las compras (reservar stock).

## Stack tecnológico

- **Java 21**
- **Spring Boot 4.1.1** (Web MVC, Data JPA, Validation, Actuator)
- **H2** en memoria (base de datos de la aplicación, ver [Limitaciones conocidas](#limitaciones-conocidas))
- **springdoc-openapi** (documentación interactiva con Swagger UI)
- **JUnit 5 + Mockito + MockMvc** (tests de controller, service y manejo de errores)
- **Testcontainers + PostgreSQL 17** (solo para el test de contexto)
- **Maven 3.9** (incluido vía `mvnw`)

> Flyway y el driver de PostgreSQL están en el `pom.xml` como preparación para una futura migración a PostgreSQL, pero hoy Flyway está desactivado (`spring.flyway.enabled=false`) y no hay scripts de migración.

## Requisitos

- Java 21 (Maven no hace falta instalarlo: el proyecto incluye `mvnw`)
- Docker, necesario para correr `./mvnw test` (el test de contexto levanta un PostgreSQL con Testcontainers)

## Cómo levantar el proyecto

La aplicación arranca por defecto en el puerto **8081** usando H2 en memoria:

```bash
./mvnw spring-boot:run
```

La app queda disponible en `http://localhost:8081`.

La documentación interactiva de la API (Swagger UI) está en:

```
http://localhost:8081/swagger-ui.html
```

El estado de salud del servicio se puede consultar en `http://localhost:8081/actuator/health`.

> **Importante:** los datos viven en memoria y **se pierden al reiniciar**. Si `producto-api` sigue con sus productos y reinicias `inventario-api`, esos productos quedarán sin inventario (consultarlo dará `404`) hasta que se vuelva a inicializar con el `POST`.

## Endpoints

| Método   | Ruta                                              | Descripción                             | Respuestas                  |
|----------|---------------------------------------------------|-----------------------------------------|-----------------------------|
| `GET`    | `/api/inventarios/producto/{productoId}`          | Consultar el inventario de un producto  | `200`, `400`, `404`         |
| `POST`   | `/api/inventarios/producto/{id}` | Inicializa stock en 0 para un producto | `200` (existente), `201` (creado) |
| `GET`    | `/api/inventarios/producto/{id}` | Consulta el stock disponible y reservado | `200`, `404` |
| `PUT`    | `/api/inventarios/producto/{id}/agregar` | Suma una cantidad al stock disponible | `200`, `400`, `404` |
| `PUT`    | `/api/inventarios/producto/{id}/reservar` | Mueve stock disponible a reservado | `200`, `400`, `404`, `409` |
| `DELETE` | `/api/inventarios/producto/{id}` | Elimina el inventario de un producto | `204` | `/api/inventarios/producto/{productoId}`          | Eliminar el inventario (idempotente)    | `204`                       |

**Validaciones** de `agregar` y `reservar`: `cantidad` obligatoria, mayor a cero y como máximo `100000` por operación.

### Detalles de comportamiento

- **`POST` idempotente:** si el inventario del producto ya existe, devuelve el existente **sin modificarlo** (conserva su stock). Responde `201` en ambos casos.
- **`DELETE` idempotente:** responde `204` exista o no el inventario.
- **`reservar`:** mueve unidades de `cantidadDisponible` a `cantidadReservada`. Si no alcanza el stock responde `409`.

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

Todas las respuestas de error son JSON. Los errores de validación devuelven un mapa `campo → mensaje`:

```json
{
  "cantidad": "La cantidad debe ser mayor a cero"
}
```

El resto usa la forma `{"error": "..."}`:

| Código | Cuándo ocurre |
|---|---|
| `400` | Datos inválidos, JSON mal formado, id no numérico o cantidad fuera de límite |
| `404` | El producto no tiene inventario |
| `409` | Stock insuficiente para reservar, conflicto de concurrencia (`@Version`: dos operaciones modificaron el mismo inventario a la vez) o violación de restricción (por ejemplo, `productoId` duplicado) |
| `500` | Error inesperado. El detalle va al log del servidor, nunca al cliente |

> Un `409` por concurrencia **no significa necesariamente que falte stock**: el cliente puede reintentar la operación.

## Correr los tests

```bash
./mvnw test
```

Los tests de integración (`InventarioControllerTest`, `InventarioApiApplicationTests`) se ejecutan **obligatoriamente con PostgreSQL usando Testcontainers** (el contenedor se comparte entre todos los tests).
Requiere tener Docker levantado localmente. Si no tienes Docker, estos tests se saltarán de forma segura gracias a la anotación `@Testcontainers(disabledWithoutDocker = true)`. 
`DocumentacionApiTest` utiliza H2 en memoria, y `InventarioServiceTest` son pruebas unitarias (Mockito) sin levantar contexto.

## Estructura del proyecto

```
src/main/java/io/github/danielmelejpinto/inventarioapi/
├── controller/     # Endpoints REST
├── service/        # Lógica de negocio
├── repository/     # Acceso a datos (Spring Data JPA)
├── model/          # Entidades JPA
├── dto/            # Objetos de entrada/salida (Request/Response)
└── exception/      # Excepciones personalizadas y manejo global

src/main/resources/
└── application.properties          # Configuración (puerto 8081, H2, Flyway desactivado)

src/test/java/.../inventarioapi/
├── controller/     # Tests de endpoints (MockMvc)
├── service/        # Tests unitarios con Mockito
├── exception/      # Tests del manejador global de errores
└── TestcontainersConfiguration.java   # PostgreSQL 17 para el test de contexto

src/test/resources/
└── application-test.properties     # Perfil test (H2 vacía)
```

## Relación con producto-api

`producto-api` usa este servicio en dos momentos:

1. **Al crear un producto:** una vez que el producto quedó **confirmado en su base de datos**, llama a `POST /api/inventarios/producto/{id}` para crear el inventario con stock en cero.
2. **Al eliminar un producto:** tras confirmar el borrado, llama a `DELETE /api/inventarios/producto/{id}`.

Estas llamadas se hacen **fuera de la transacción de `producto-api`** y se reintentan si fallan, por lo que la consistencia entre ambos servicios es **eventual**: durante un instante (o mientras `inventario-api` esté caído) un producto puede existir sin inventario. Para que los reintentos sean seguros, `POST` y `DELETE` son **idempotentes**.

> Los productos de prueba que `producto-api` genera en su perfil `dev` no pasan por este flujo, así que no tienen inventario.

## Limitaciones conocidas

- **Sin autenticación ni autorización.** Cualquiera con acceso al puerto 8081 puede agregar, reservar o borrar stock; este servicio no debería exponerse fuera de la red interna.
- **Datos volátiles:** solo usa H2 en memoria; se pierden al reiniciar. Si `producto-api` también reinicia su base y reutiliza ids, `POST` devolverá el inventario antiguo con su stock en lugar de uno nuevo.
- **Reservas sin identificador:** no existe una reserva como entidad. No se puede liberar, confirmar tras un pago ni caducar, y reintentar `reservar` tras un timeout **reserva dos veces**.
- **Conflictos de concurrencia sin reintento interno:** dos operaciones simultáneas sobre el mismo inventario pueden dar `409` aunque haya stock suficiente; el cliente debe reintentar.
- **Eliminación insegura:** `DELETE` borra el inventario aunque tenga reservas activas.
- **`POST` responde siempre `201`,** incluso cuando el inventario ya existía.
- **Sin migraciones:** el esquema lo genera Hibernate (`ddl-auto=update`); Flyway está preparado pero desactivado.

## Autor

**Daniel Melej Pinto**