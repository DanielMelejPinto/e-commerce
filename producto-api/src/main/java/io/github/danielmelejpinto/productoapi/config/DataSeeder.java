package io.github.danielmelejpinto.productoapi.config;

import java.math.BigDecimal;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.github.danielmelejpinto.productoapi.model.Producto;
import io.github.danielmelejpinto.productoapi.repository.ProductoRepository;
import net.datafaker.Faker;

@Configuration
@Profile("dev") // solo corre en desarrollo, nunca en docker/producción
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    @Bean
    CommandLineRunner cargarDatosDePrueba(ProductoRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                log.info("Ya existen productos, se omite la carga de datos de prueba");
                return;
            }

            Faker faker = new Faker();

            IntStream.range(0, 20).forEach(i -> {
                Producto producto = new Producto();
                producto.setNombre(faker.commerce().productName());
                producto.setPrecio(BigDecimal.valueOf(faker.number().randomDouble(2, 5, 500)));
                producto.setEstado(io.github.danielmelejpinto.productoapi.model.EstadoProducto.ACTIVO);
                repository.save(producto);
            });

            log.info("20 productos de prueba generados con Datafaker");
        };
    }
}