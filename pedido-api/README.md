# pedido-api

Microservicio de pedidos del [e-commerce](../README.md). Orquesta la compra: valida al usuario con JWT, consulta el precio real en [`producto-api`](../producto-api), reserva stock en [`inventario-api`](../inventario-api) y guarda el pedido. Construido con **Java 21** y **Spring Boot 4.1.1**; se comunica de forma síncrona con `RestClient`.

## Características

- **Creación de pedidos** autenticados: recibe una lista de `productoId` y `cantidad`.
- **Usuario tomado del token:** el `userId` sale del JWT, no del cuerpo de la solicitud.
- **Precio real:** por cada ítem hace `GET` a `producto-api` y guarda el `precioUnitario` vigente en ese momento.
- **Reserva de stock:** por cada ítem llama a `PUT .../reservar` de `inventario-api`.
- **Compensación (saga):** si algo falla, llama a `PUT .../liberar` por cada ítem ya reservado, relanza el error y no guarda el pedido.
- **Historial:** lista los pedidos del usuario autenticado.
- **Persistencia:** H2 en memoria (`Pedido` y `PedidoItem` con JPA); se pierde al reiniciar.

## Requisitos

- **JDK 21**.
- `producto-api` (8080) e `inventario-api` (8081) levantados; si no, crear pedidos falla por errores de conexión.
- Un **token JWT** emitido por [`usuario-api`](../usuario-api) y el **mismo secreto de firma** configurado aquí.

## Cómo levantar el proyecto

`pedido-api` **no define un valor por defecto** para `seguridad.jwt.secret`: sin él, la aplicación no arranca. Usa el mismo secreto que `usuario-api` (`JWT_SECRET`), por ejemplo con una variable de entorno:

```bash
export JWT_SECRET='pon_aqui_un_secreto_largo_de_al_menos_32_caracteres'   # para usuario-api
export SEGURIDAD_JWT_SECRET="$JWT_SECRET"                                # para pedido-api
./mvnw spring-boot:run
```

En PowerShell: `.\mvnw.cmd spring-boot:run`. También puedes pasarlo como argumento:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--seguridad.jwt.secret=$JWT_SECRET"
```

Queda en `http://localhost:8083`.

> **Todo el servicio exige JWT.** Swagger UI (`/swagger-ui.html`) y la consola H2 (`/h2-console`) están habilitados en la configuración, pero la seguridad los bloquea sin token, así que en la práctica no se pueden abrir desde el navegador. Usa `curl` o el frontend.

## Endpoints

| Método | Ruta | Descripción | Respuestas |
|---|---|---|---|
| `POST` | `/api/pedidos` | Crear un pedido del usuario autenticado | `201`, `400` |
| `GET` | `/api/pedidos/mis-pedidos` | Listar los pedidos del usuario autenticado | `200` |

Ambos requieren `Authorization: Bearer <token>`.

**Validaciones:** `items` no puede estar vacío; cada ítem exige `productoId` y `cantidad` (mínimo 1).

### Ejemplo

```bash
curl -X POST http://localhost:8083/api/pedidos \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"items":[{"productoId":21,"cantidad":3}]}'
```

Respuesta (`201 Created`, ilustrativa):

```json
{
  "id": 1,
  "usuarioId": 1,
  "estado": "CONFIRMADO",
  "total": 149970.00,
  "fechaCreacion": "2026-09-29T10:20:00",
  "items": [
    { "id": 1, "productoId": 21, "cantidad": 3, "precioUnitario": 49990.00, "subtotal": 149970.00 }
  ]
}
```

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8083/api/pedidos/mis-pedidos
```

## Flujo de creación

1. El filtro JWT valida el token y deja el `userId` como usuario autenticado (sin consultar a `usuario-api`).
2. Para cada ítem, en orden: `GET` del producto y `PUT .../reservar` en inventario.
3. Si todos los ítems se reservan, calcula el total, marca el pedido `CONFIRMADO` y lo guarda.
4. Si falla cualquier ítem, libera las reservas ya hechas, relanza la excepción y el pedido **no se guarda**.

## Configuración

| Propiedad | Predeterminado | Descripción |
|---|---|---|
| `server.port` | `8083` | Puerto HTTP |
| `seguridad.jwt.secret` | **Sin valor** | Secreto de firma; debe coincidir con `usuario-api` |
| `api.producto.url` | `http://localhost:8080/api/productos` | Base de `producto-api` |
| `api.inventario.url` | `http://localhost:8081/api/inventarios` | Base de `inventario-api` |
| `api.usuario.url` | `http://localhost:8082/api/usuarios` | Definida, pero el código actual no la usa |
| `spring.datasource.url` | `jdbc:h2:mem:pedidodb` | Base H2 en memoria |

## Correr los tests

```bash
./mvnw test
```

Incluye pruebas del servicio (`PedidoServiceTest`) y del controlador (`PedidoControllerTest`). La cobertura es acotada (pocos casos); no cubre el flujo completo entre servicios.

## Estructura del proyecto

```
src/main/java/io/github/danielmelejpinto/pedidoapi/
├── client/         # ProductoClient, InventarioClient (RestClient) y sus DTOs
├── controller/     # PedidoController
├── dto/            # PedidoRequest, PedidoItemRequest, PedidoResponse, PedidoItemResponse
├── model/          # Pedido, PedidoItem, EstadoPedido
├── repository/     # PedidoRepository
├── security/       # JwtService, JwtAuthenticationFilter, SecurityConfig
└── service/        # PedidoService
```

## Limitaciones conocidas

- **Sin manejador global de errores:** las fallas de `producto-api` o `inventario-api` (producto inexistente, stock insuficiente, servicio caído) no se traducen a respuestas claras; el cliente recibe el error genérico del framework.
- **Compensación de mejor esfuerzo:** si `liberar` falla, el error solo se escribe en `System.err` y el stock queda reservado sin pedido.
- **Sin idempotencia:** reintentar `POST /api/pedidos` tras un timeout puede reservar stock dos veces.
- **Ciclo de vida mínimo:** el estado final es siempre `CONFIRMADO`; no hay pago, cancelación ni confirmación posterior (existe `CANCELADO` en el enum, pero ningún flujo lo usa).
- **Sin roles** en las autoridades del usuario autenticado.
- **Secreto JWT sin valor por defecto** y compartido manualmente con `usuario-api`.
- **H2 en memoria:** los pedidos se pierden al reiniciar.
- Llamadas HTTP sin timeouts configurados explícitamente.

## Autor

**Daniel Melej Pinto** · [GitHub](https://github.com/DanielMelejPinto)