# E-Commerce Microservices Portfolio 

[![CI](https://github.com/DanielMelejPinto/e-commerce/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/DanielMelejPinto/e-commerce/actions)
![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.1-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white)
![React](https://img.shields.io/badge/React-19-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)

Este es un proyecto *full-stack* y *cloud-native* que implementa una plataforma de E-Commerce basada en una arquitectura de **microservicios con Spring Boot 4.1.1** y un **frontend SPA con React 19**.

Ha sido desarrollado metódicamente aplicando patrones de diseño avanzados, buenas prácticas de DevOps, seguridad y rendimiento, con el objetivo de demostrar un nivel de ingeniería de software de calidad *senior*.

## Arquitectura

```mermaid
flowchart TD
    Client([Cliente Web / React])
    Gateway[Gateway API\n:8000]
    
    subgraph Núcleo
        Usuario[Usuario API\n:8082]
        Producto[Producto API\n:8080]
        Inventario[Inventario API\n:8081]
        Pedido[Pedido API\n:8083]
        
        DB_U[(Usuario DB)]
        DB_P[(Producto DB)]
        DB_I[(Inventario DB)]
        DB_Pe[(Pedido DB)]
        
        Usuario --> DB_U
        Producto --> DB_P
        Inventario --> DB_I
        Pedido --> DB_Pe
    end
    
    subgraph Extras
        Kafka[[Apache Kafka]]
        Prometheus((Prometheus))
    end
    
    Client -->|HTTP| Gateway
    Gateway --> Usuario
    Gateway --> Producto
    Gateway --> Inventario
    Gateway --> Pedido
    
    Pedido -->|HTTP Síncrono| Producto
    Pedido -->|HTTP Síncrono| Inventario
    
    Producto -->|Outbox Pattern| Kafka
    Kafka -->|Eventos| Inventario
    
    Prometheus -.->|Scrape| Usuario
    Prometheus -.->|Scrape| Producto
    Prometheus -.->|Scrape| Inventario
    Prometheus -.->|Scrape| Pedido
```

### Servicios Core (Núcleo)
1. **`gateway-api` (Puerto 8000)**: API Gateway (Spring Cloud Gateway) que centraliza el enrutamiento, CORS y simplifica el acceso.
2. **`usuario-api` (Puerto 8082)**: Gestión de identidades, roles (USER, ADMIN) y validación JWT.
3. **`producto-api` (Puerto 8080)**: Catálogo de productos. 
4. **`inventario-api` (Puerto 8081)**: Gestión de stock. Maneja concurrencia de reposición mediante **Optimistic Locking** (`@Version`).
5. **`pedido-api` (Puerto 8083)**: Orquestador central de la **Saga Síncrona**.

### Componentes de Soporte (Extras)
- **Apache Kafka + Patrón Outbox (ShedLock)**: Propaga eventos de creación de productos de `producto-api` a `inventario-api` de forma resiliente para inicializar el stock.
- **Circuit Breaker (Resilience4j)**: Protege a `pedido-api` para reaccionar con Fail-Fast si un servicio dependiente está caído.
- **Prometheus**: Recolección de métricas a través de Spring Boot Actuator.

## Decisiones de Diseño y Límites Conocidos

1. **Carrera de Cancelación de Pedidos**: Al cancelar un pedido concurrentemente, existía el riesgo de liberar el stock múltiples veces si la lectura del estado `CONFIRMADO` y el guardado `CANCELADO` se solapaban. 
   - *Por qué no `@Version`:* El `@Version` en `Pedido` evitaría sobreescribir el estado, pero la excepción `ObjectOptimisticLockingFailureException` saltaría *después* de que la llamada a la API externa de inventario ya hubiera liberado el stock.
   - *Solución implementada:* Se utiliza un `UPDATE` condicional atómico (`UPDATE Pedido p SET p.estado = CANCELADO WHERE p.id = :id AND p.estado = CONFIRMADO`). Si devuelve 1 fila afectada, ganamos la carrera y liberamos stock; si devuelve 0, evitamos tocar el stock externo. Verificado con tests de concurrencia en H2 y PostgreSQL.
2. **Saga Orquestada Síncrona**: `pedido-api` orquesta la transacción llamando a `inventario-api` por HTTP. Si algo falla (ej. base de datos de pedidos local), ejecuta la compensación liberando el stock previamente reservado. 
   - *Límites:* Si la red cae justo antes de la compensación, quedarán reservas huérfanas en el inventario, ya que la compensación no tiene un mecanismo de reintento en background (Outbox) implementado actualmente.
3. **Idempotencia con Scope por Usuario**: La API de pedidos previene cobros duplicados mediante `Idempotency-Key`. La clave primaria de la tabla de idempotencia es compuesta (`usuario_id, clave`), garantizando que un usuario malintencionado no pueda adivinar una clave en uso para interceptar pedidos ajenos.

## Roadmap / Deuda Técnica (No implementado)

- **Eliminar dependencias de Actuator/Prometheus**: Los `HEALTHCHECK` del Dockerfile acoplan el inicio de los contenedores a Actuator, pero podrían eliminarse para ahorrar memoria.
- **Exposición innecesaria de Actuator**: `/actuator/**` está abierto y expone detalles sensibles del servidor.
- **Rediseño Completo de la Saga**: Actualmente, la creación del pedido falla síncronamente. Un diseño más resiliente crearía el pedido en estado `PENDIENTE` primero, reservaría el stock asíncronamente y pasaría a `CONFIRMADO`.
- **Falta de Dead Letter Queue (DLQ) en compensaciones**: Las fallas al compensar stock se loguean, pero no se encolan para revisión automática, pudiendo dejar stock congelado indefinidamente.
- **Arranque en frío de Kafka / Retención de Eventos**: Un evento de outbox fallido deja el producto `PENDIENTE` permanentemente sin reintento manual; la conexión a Kafka puede demorar ~5m en registrar metadata.

## Cómo ejecutar el proyecto en local

Puedes levantar absolutamente todo (PostgreSQL, Kafka, métricas, frontend y backend) con un solo comando:

```bash
cp .env.example .env
docker compose up -d --build
```
Una vez que los contenedores estén `healthy`, accede a `http://localhost:5173`. Para apagar: `docker compose down`.

> **⚠️ IMPORTANTE - Entornos y Seguridad:**
> - El archivo `.env` controla los secretos de BD y JWT del entorno productivo/docker. **Si falta la variable de BD, el compose fallará.**
> - El perfil `dev` de Spring Boot utiliza una base de datos H2 en memoria, y expone credenciales hardcodeadas (como `admin123`) y un secreto JWT débil. Estas credenciales **SOLO son válidas y funcionan bajo el perfil `dev`** y nunca en el despliegue Docker/PostgreSQL.

---
**Autor:** [Daniel Melej Pinto](https://github.com/DanielMelejPinto)
