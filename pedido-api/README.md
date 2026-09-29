# E-Commerce · Servicio de Pedidos (`pedido-api`)

Microservicio encargado de orquestar el proceso de compra, consolidando el catálogo, el inventario y al usuario en una orden formal.

Este servicio está construido con **Java 21** y **Spring Boot 4.1.1** y se comunica de manera síncrona con el resto del ecosistema mediante `RestClient`.

## Características principales

- **Creación de Pedidos**: Recibe una orden con los identificadores de productos y las cantidades deseadas.
- **Comunicación entre servicios**: 
  - Realiza un `GET` hacia `producto-api` para obtener el precio real y actualizado de los productos.
  - Realiza un `PUT` hacia `inventario-api` para reservar atómicamente el stock necesario.
- **Consistencia**: El pedido final se calcula y se guarda en base de datos únicamente si el stock pudo ser reservado exitosamente.
- **Persistencia local**: Base de datos H2 para desarrollo y pruebas, con estructura JPA completa (`Pedido` y `PedidoItem`).

## Ejecución Local

### Requisitos
- **JDK 21**
- Los servicios de **Producto** e **Inventario** deben estar ejecutándose en sus respectivos puertos (8080 y 8081) para que el flujo completo no devuelva errores de conexión.

### Iniciar el servicio

Desde la raíz de la carpeta `pedido-api`, ejecuta:

```bash
./mvnw spring-boot:run
```
*(En Windows utiliza `.\mvnw.cmd spring-boot:run`)*

### Acceso
- **API Base**: `http://localhost:8083/api/pedidos`
- **Documentación Swagger UI**: `http://localhost:8083/swagger-ui.html`
- **Consola H2**: `http://localhost:8083/h2-console` (URL JDBC: `jdbc:h2:mem:pedidodb`)

## Tecnologías Principales

- **Spring WebMVC & RestClient**: Para exponer la API y consumir las otras APIs internas.
- **Spring Data JPA**: Acceso a datos relacionales.
- **Jakarta Validation**: Asegura que las solicitudes contengan los items y las cantidades correctas.
- **Springdoc OpenAPI**: Autogeneración de documentación y cliente de pruebas integrado.
