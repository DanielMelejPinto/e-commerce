package io.github.danielmelejpinto.productoapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class OpenApiConfig {

    // Datos que aparecen en la cabecera de Swagger UI
    @Bean
    public OpenAPI productoApiOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Producto API")
                .description("Microservicio de productos del e-commerce")
                .version("0.0.1"));
    }
}