# E-Commerce Microservices Portfolio 

![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.1-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![React](https://img.shields.io/badge/React-19-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![TypeScript](https://img.shields.io/badge/TypeScript-5.7-3178C6?style=for-the-badge&logo=typescript&logoColor=white)

Este es un proyecto *full-stack* y *cloud-native* que implementa una plataforma de E-Commerce basada en una arquitectura de **microservicios con Spring Boot 21** y un **frontend SPA con React 19**.

Ha sido desarrollado metódicamente a través de **14 fases de refactorización y mejora continua**, aplicando patrones de diseño avanzados, buenas prácticas de DevOps, seguridad y rendimiento, con el objetivo de demostrar un nivel de ingeniería de software de calidad *senior*.

##  Arquitectura de Microservicios

El backend está compuesto por 4 microservicios independientes que se comunican mediante HTTP REST y patrones de consistencia eventual.

1. **`usuario-api` (Puerto 8082)**: Gestión de identidades, roles (USER, ADMIN) y generación de JWT.
2. **`producto-api` (Puerto 8080)**: Catálogo de productos. Utiliza un **Patrón Outbox (ShedLock)** para propagar eventos de creación de productos hacia otros servicios de manera resiliente.
3. **`inventario-api` (Puerto 8081)**: Gestión de stock (disponible y reservado). Maneja concurrencia mediante **Optimistic Locking** (`@Version`).
4. **`pedido-api` (Puerto 8083)**: Orquestador central. Implementa el patrón **Saga Coreografiada (Compensación)** y protección contra retrys mediante **Idempotency Keys**.

###  Patrones y Prácticas Implementadas (Backend)
- **Seguridad Centralizada (JWT)**: Todos los microservicios validan un JWT simétrico para rutas protegidas (`ADMIN` para escritura, `USER` para compras).
- **Patrón Outbox y ShedLock**: En `producto-api` para garantizar la entrega "al menos una vez" de eventos hacia `inventario-api`, con soporte para concurrencia horizontal (ShedLock sobre tabla de base de datos).
- **Manejo de Transacciones Distribuidas**: `pedido-api` orquesta la reserva de stock. Si hay un fallo posterior (por ej. base de datos caída), ejecuta rutinas de compensación (liberación de stock).
- **Idempotencia Transaccional**: La creación de pedidos requiere una cabecera `Idempotency-Key` respaldada por una tabla de Redis/DB para evitar duplicidad de cobros o doble reserva en reintentos de red.
- **Resiliencia (Retry / Backoff)**: Interacciones HTTP reforzadas y propagación de errores semánticos (traducción de `404`, `409` usando `@RestControllerAdvice`).
- **Migraciones (Flyway)**: Control de versiones de esquema de base de datos para entornos reproducibles.
- **Observabilidad**: Integración nativa con **Spring Boot Actuator** y **Prometheus** (`/actuator/prometheus`) para monitoreo de métricas.

##  Frontend (React 19 + TypeScript + Vite)

El frontend (`frontend-app`) se rediseñó bajo estándares estrictos de la industria:
- **Tipado Estricto (TypeScript)**: `verbatimModuleSyntax` habilitado, interfaces globales y 0% de uso de `any`.
- **Capa de Servicios**: Desacoplamiento total entre componentes UI (`.tsx`) e integraciones HTTP (`axios`).
- **Performance & UX**: Renderizado en tiempo real (Catálogo con `useMemo`), *Skeleton Loaders*, y UI states reactivos (sin bloqueos de red bruscos).
- **Accesibilidad (a11y)**: HTML Semántico (`<main>`, `<article>`, `<aside>`), y etiquetas ARIA (`aria-label`, `aria-busy`, `role="alert"`) orientadas a accesibilidad web (Lighthouse friendly).
- **Testing (Vitest + Testing Library)**: Tests unitarios al contexto de la aplicación, emulando el DOM con `jsdom` para asegurar el flujo de la canasta de compras.

##  Cómo ejecutar el proyecto en local

### 1. Iniciar Bases de Datos (PostgreSQL)
Asegúrate de tener Docker instalado y ejecutándose:
```bash
docker-compose up -d
```

### 2. Iniciar Backend (Microservicios)
Se debe definir el mismo secreto JWT para todos los servicios. Puedes ejecutar cada uno en terminales separadas:
```bash
export SEGURIDAD_JWT_SECRET=super_secreto_para_desarrollo_local_256bits
(cd usuario-api && ./mvnw spring-boot:run)
(cd producto-api && ./mvnw spring-boot:run)
(cd inventario-api && ./mvnw spring-boot:run)
(cd pedido-api && ./mvnw spring-boot:run)
```

### 3. Iniciar Frontend
```bash
cd frontend-app
npm install
npm run dev
```
La aplicación estará disponible en `http://localhost:5173`.

##  Pruebas y CI/CD (GitHub Actions)

El repositorio incluye automatización completa de integración continua (CI) en `.github/workflows/`:
1. **`ci.yml` (Backend)**: Compila y ejecuta la suite de pruebas unitarias y de integración (vía *Testcontainers*) para los 4 módulos de Java con JDK 21.
2. **`frontend-ci.yml` (Frontend)**: Realiza linting (`oxlint`), ejecuta pruebas unitarias (`vitest`) y verifica el *build* de producción para la rama `main`.

Comandos locales:
```bash
# Backend (dentro de cada módulo)
./mvnw clean verify

# Frontend
npm run lint
npm run test
npm run build
```

---
*Desarrollado como demostración de arquitectura, código limpio e ingeniería de software moderna.*
**Autor:** [Daniel Melej Pinto](https://github.com/DanielMelejPinto)
