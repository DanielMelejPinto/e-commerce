package io.github.danielmelejpinto.productoapi;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProductoApiApplicationTests {
    @MockitoBean
    private KafkaTemplate<String, Object> kafkaTemplate;



	@Test
	void contextLoads() {
	}

}
