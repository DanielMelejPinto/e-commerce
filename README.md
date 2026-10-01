# E-Commerce Microservices Portfolio 

![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white)
![React](https://img.shields.io/badge/React-19-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![TypeScript](https://img.shields.io/badge/TypeScript-5.7-3178C6?style=for-the-badge&logo=typescript&logoColor=white)

Este es un proyecto *full-stack* y *cloud-native* que implementa una plataforma de E-Commerce basada en una arquitectura de **microservicios con Spring Boot 4.1.1** y un **frontend SPA con React 19**.

Ha sido desarrollado metódicamente aplicando patrones de diseño avanzados, buenas prácticas de DevOps, seguridad y rendimiento, con el objetivo de demostrar un nivel de ingeniería de software de calidad *senior*.

## Arquitectura de Microservicios

El backend está compuesto por 5 microservicios independientes detrás de un API Gateway, que se comunican mediante REST, Circuit Breakers y mensajería asíncrona con Apache Kafka.

1. **`gateway-api` (Puerto 8000)**: API Gateway (Spring Cloud Gateway) que centraliza el enrutamiento, CORS y simplifica el acceso desde el frontend.
2. **`usuario-api` (Puerto 8082)**: Gestión de identidades, roles (USER, ADMIN) y generación de JWT.
3. **`producto-api` (Puerto 8080)**: Catálogo de productos. Utiliza **Apache Kafka** y un **Patrón Outbox (ShedLock)** para propagar eventos de forma resiliente.
4. **`inventario-api` (Puerto 8081)**: Gestión de stock. Consume eventos de Kafka y maneja concurrencia mediante **Optimistic Locking** (`@Version`).
5. **`pedido-api` (Puerto 8083)**: Orquestador central. Implementa el patrón **Saga Coreografiada** y **Circuit Breakers (Resilience4j)** para proteger la comunicación con otros servicios.

### Patrones y Prácticas Implementadas (Backend)
- **Event-Driven Architecture (EDA)**: Desacoplamiento de microservicios usando Apache Kafka para la sincronización de inventario tras la creación de productos.
- **Circuit Breaker y Resilience4j**: Protección en `pedido-api` para reaccionar inmediatamente (Fail-Fast) si `inventario-api` no está disponible, evitando cuellos de botella.
- **Seguridad Centralizada (JWT)**: Validaciones de JWT simétrico para rutas protegidas (`ADMIN` para escritura, `USER` para compras).
- **Manejo de Transacciones Distribuidas**: `pedido-api` orquesta la reserva de stock y ejecuta rutinas de compensación si la orden falla.
- **Migraciones (Flyway)**: Control de versiones de esquema de base de datos para entornos reproducibles.
- **Testing**: Pruebas de integración automatizadas usando **Testcontainers** (PostgreSQL 17).

## Frontend (React 19 + TypeScript + Vite)

El frontend (`frontend-app`) está diseñado para operar a escala empresarial:
- **Server-State Management**: Integración de **React Query (TanStack Query)** para el manejo de fetching, caché, reintentos automáticos y sincronización de datos.
- **Client-State Management**: Uso de **Zustand** para la gestión global del Carrito de Compras de forma optimizada sin re-renders innecesarios.
- **Server-Side Pagination & Search**: Búsqueda delegada a la base de datos (PostgreSQL) para soportar catálogos masivos sin saturar la memoria del navegador.
- **Tipado Estricto (TypeScript)**: Interfaces globales y 0% de uso de `any`.
- **Capa de Servicios**: Desacoplamiento total entre componentes UI (`.tsx`) e integraciones HTTP (`axios`).

## Puertos

| Servicio | Puerto | Notas |
|---|---|---|
| `frontend-app` | 5173 | nginx; proxea `/api/` al gateway |
| `gateway-api` | 8000 | Punto de entrada a la API |
| `producto-api` | 8080 | |
| `inventario-api` | 8081 | |
| `usuario-api` | 8082 | |
| `pedido-api` | 8083 | |
| PostgreSQL | 5432 | Bases `producto_db`, `inventario_db`, `usuario_db`, `pedido_db` |
| Kafka | 9092 | Desde el host. Entre contenedores: `kafka:29092` |
| Prometheus | 9090 | Targets en http://localhost:9090/targets |

## Perfiles

- **`dev` (por defecto)**: H2 en memoria, sin PostgreSQL. Para `producto-api` e `inventario-api` hace falta Kafka en `localhost:9092` (`docker compose up -d kafka`).
- **`docker`**: PostgreSQL, Flyway, `ddl-auto=validate`, Swagger y consola H2 desactivados. No tiene valor por defecto para `JWT_SECRET`.

## Cómo ejecutar el proyecto en local

### 1. Variables de entorno
```bash
cp .env.example .env
```
Edita `.env` (nunca se sube al repo):

| Variable | Descripción |
|---|---|
| `JWT_SECRET` | Mínimo 32 caracteres. Mismo valor para todos los servicios |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | Credenciales de PostgreSQL |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | Administrador inicial de `usuario-api` (opcional) |

### 2. Todo con Docker Compose
```bash
docker compose up -d --build
docker compose ps
```
Levanta PostgreSQL, Kafka, Prometheus, los 5 servicios y el frontend. Cuando todo esté `healthy`, abre http://localhost:5173. Para apagar: `docker compose down` (con `-v` borra también los datos).

### 3. Backend en modo desarrollo (sin Docker)
En terminales separadas, con Kafka arriba si usas producto e inventario:
```bash
(cd gateway-api && ./mvnw spring-boot:run)
(cd usuario-api && ./mvnw spring-boot:run)
(cd producto-api && ./mvnw spring-boot:run)
(cd inventario-api && ./mvnw spring-boot:run)
(cd pedido-api && ./mvnw spring-boot:run)
```

### 4. Frontend en modo desarrollo
```bash
cd frontend-app
npm install
npm run dev
```
Vite (5173) proxea `/api` al gateway en `http://localhost:8000`.

## Endpoints principales (vía gateway, `:8000`)

Las rutas protegidas requieren `Authorization: Bearer <token>`. El token sale del login (`{"token": "..."}`).

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/usuarios/registro` | Registro (`nombre`, `email`, `password` de 8 a 72 caracteres) |
| POST | `/api/usuarios/login` | Login (`email`, `password`) |
| GET | `/api/usuarios/me` | Usuario autenticado |
| GET / POST | `/api/productos` | Listar / crear producto (`nombre`, `descripcion`, `precio`) |
| GET / PUT / DELETE | `/api/productos/{id}` | Detalle / editar / dar de baja |
| GET / POST | `/api/inventarios/producto/{productoId}` | Consultar / inicializar inventario |
| PUT | `/api/inventarios/producto/{productoId}/agregar` | Agregar stock (`cantidad`) |
| PUT | `/api/inventarios/producto/{productoId}/reservar` | Reservar stock |
| PUT | `/api/inventarios/producto/{productoId}/liberar` | Liberar stock |
| POST | `/api/pedidos` | Crear pedido (`items: [{productoId, cantidad}]`) |
| GET | `/api/pedidos/mis-pedidos` | Pedidos del usuario autenticado |
| POST | `/api/pedidos/{id}/cancelar` | Cancelar pedido (libera el stock) |

## Pruebas

El proyecto cuenta con suites de tests rigurosas (Unitarias e Integración) usando **JUnit 5**, **Mockito** y **Testcontainers** en el ecosistema Spring Boot 4.

Comando local para probar cualquier microservicio:
```bash
./mvnw clean verify
```

---
**Autor:** [Daniel Melej Pinto](https://github.com/DanielMelejPinto)
