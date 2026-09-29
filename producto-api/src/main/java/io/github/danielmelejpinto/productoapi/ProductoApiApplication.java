package io.github.danielmelejpinto.productoapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ProductoApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProductoApiApplication.class, args);
	}
}