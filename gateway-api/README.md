# Gateway API

Microservicio que actúa como API Gateway del E-commerce. Enruta todas las peticiones a los distintos microservicios backend y maneja CORS para el frontend.

## Stack
- Java 21
- Spring Boot 3.2.x
- Spring Cloud Gateway

## Funcionalidades
- Enrutamiento a `usuario-api`, `producto-api`, `inventario-api`, `pedido-api`
- Configuración de CORS permitiendo solicitudes desde el frontend local (`localhost:5173`)
- Permite la visualización de Swagger UI agregada

## Ejecución local
```bash
mvn spring-boot:run
```

Para conectarlo a la red local de desarrollo, usa el perfil `dev` o simplemente inícialo con los demás servicios ya que las rutas por defecto apuntan a localhost.
