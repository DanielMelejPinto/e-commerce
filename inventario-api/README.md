# Inventario API

Microservicio de inventario del e-commerce.

## Descripción

Gestiona las existencias y reservas de stock por producto, trabajando de la mano con `producto-api`.

## Stack tecnológico

- Java 21
- Spring Boot 4.1.1
- Spring Data JPA + H2 (base de datos en memoria)
- Maven 3.9
- Testcontainers (para testeo de contexto con PostgreSQL)

## Requisitos

- Java 21
- Docker (opcional, solo necesario para correr el test de contexto con Testcontainers)

## Cómo levantar el proyecto

La aplicación arranca por defecto en el puerto 8081 usando H2 en memoria.

```bash
./mvnw spring-boot:run
```

La app queda disponible en `http://localhost:8081`. 

La documentación interactiva de la API con Swagger UI está disponible en:
```
http://localhost:8081/swagger-ui.html
```

## Endpoints

| Método   | Ruta                                            | Descripción                             | Respuestas                  |
|----------|-------------------------------------------------|-----------------------------------------|-----------------------------|
| `GET`    | `/api/inventarios/producto/{productoId}`          | Consultar el inventario de un producto  | `200`, `400`, `404`         |
| `POST`   | `/api/inventarios/producto/{productoId}`          | Inicializar el inventario (idempotente) | `201`                       |
| `PUT`    | `/api/inventarios/producto/{productoId}/agregar`  | Agregar stock                           | `200`, `400`, `404`         |
| `PUT`    | `/api/inventarios/producto/{productoId}/reservar` | Reservar stock                          | `200`, `400`, `404`, `409`  |
| `DELETE` | `/api/inventarios/producto/{productoId}`          | Eliminar el inventario                  | `204`                       |

## Manejo de errores

Los errores de validación de cuerpo y tipos devuelven respuestas HTTP acordes (`400 Bad Request`). Para los de negocio usamos mensajes específicos:

| Código | Cuándo ocurre |
|---|---|
| `400` | Datos inválidos, JSON mal formado, id no numérico o valores negativos/fuera de límite |
| `404` | El producto no tiene inventario |
| `409` | Stock insuficiente para reservar, o dos operaciones modificaron el mismo inventario a la vez (`@Version`), o violación de restricción |
| `500` | Error inesperado |

## Correr los tests

```bash
./mvnw test
```

A diferencia de `producto-api`, el test de contexto de `inventario-api` usa Testcontainers, por lo que **necesita Docker corriendo**. En los tests unitarios (con perfil `test`) se usa una base en memoria H2.

## Estructura del proyecto

```
src/main/java/io/github/danielmelejpinto/inventarioapi/
├── controller/     # Endpoints REST
├── service/        # Lógica de negocio
├── repository/     # Acceso a datos (Spring Data JPA)
├── model/          # Entidades JPA
├── dto/            # Objetos de entrada/salida (Request/Response)
├── exception/      # Excepciones personalizadas y manejo global
└── config/         # Configuración y OpenAPI/Swagger

src/main/resources/
└── application.properties          # Configuración común (puerto 8081, H2, Flyway desactivado en dev)

src/test/resources/
└── application-test.properties     # Perfil test (H2 vacía)
```

## Relación con producto-api

`inventario-api` es llamado por `producto-api` en dos momentos clave:
1. **Al crear un producto**: se inicializa el inventario con cantidad 0 (`POST`).
2. **Al eliminar un producto**: se elimina el inventario correspondiente (`DELETE`).

## Limitaciones conocidas

- **Sin autenticación ni autorización.**
- **Reservas sin liberar/confirmar:** el sistema reserva el stock pero no hay mecanismo para liberar reservas caducadas o confirmarlas tras un pago.
- **Base de datos:** por ahora usa solo H2.
- **Eliminación insegura:** el método `DELETE` elimina el inventario independientemente de si tiene o no reservas activas.
