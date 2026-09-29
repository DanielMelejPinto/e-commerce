# E-Commerce Microservicios

Este repositorio contiene el backend de un sistema de e-commerce básico compuesto por dos microservicios desarrollados en **Java 21** con **Spring Boot 4.1.1**.

## Arquitectura

El sistema adopta una arquitectura de microservicios, comunicándose mediante HTTP asíncrono con el patrón **Outbox**.

1. **[producto-api](./producto-api)**: Encargado de gestionar el catálogo de productos (creación, lectura, actualización y borrado lógico).
2. **[inventario-api](./inventario-api)**: Encargado de gestionar el stock de los productos (inicialización, reservas y agregados).

### Flujo de Integración (Patrón Outbox)

Para garantizar consistencia y resiliencia en la red sin recurrir a transacciones distribuidas:
- Cuando un producto es creado en `producto-api`, queda en estado `PENDIENTE`. En la misma transacción local de base de datos se guarda un evento de inicialización en una tabla "outbox".
- Un procesador en segundo plano (Scheduler) se encarga de leer la tabla outbox y enviar la petición REST asíncrona hacia `inventario-api`.
- Si `inventario-api` procesa con éxito la petición, el producto en `producto-api` pasa a estado `ACTIVO` y se hace visible. En caso de fallos de red (`5xx` / timeouts), la petición se reintenta automáticamente.

## Requisitos Previos

- **Java 21**
- **Docker** (opcional, pero fuertemente recomendado para ejecutar las pruebas de integración que emplean **Testcontainers** con PostgreSQL 17).
- No es necesario tener Maven instalado, ya que cada módulo incorpora el Maven Wrapper (`mvnw`).

## Ejecución Local

Ambos servicios utilizan **H2 en memoria** por defecto, lo que hace que arrancar el proyecto sea sumamente fácil para desarrollo, pero los datos se pierden al reiniciar.

1. **Arrancar Inventario API** (Corre en el puerto 8081)
   ```bash
   cd inventario-api
   ./mvnw spring-boot:run
   ```

2. **Arrancar Producto API** (Corre en el puerto 8080)
   ```bash
   cd producto-api
   ./mvnw spring-boot:run
   ```

Al arrancar `producto-api` con el perfil por defecto (`dev`), se insertarán automáticamente 20 productos de prueba empleando Datafaker.
*Nota: Los productos creados por el perfil dev no pasan por el pipeline Outbox, así que no tendrán inventario inicializado.*

## Documentación API

Cada microservicio expone dinámicamente su documentación OpenAPI (Swagger UI):

- **Productos:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Inventario:** [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)

## Ejecutar Pruebas

Para ejecutar las baterías completas de test (que cubren Controller, Service e Integración), dirígete a la carpeta de cada proyecto y ejecuta:

```bash
./mvnw test
```

- Los tests de `producto-api` corren en **H2**.
- Los tests de `inventario-api` utilizan **Testcontainers (PostgreSQL)** para validar restricciones y concurrencia. Si Docker no está corriendo, se saltarán automáticamente.
