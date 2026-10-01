package io.github.danielmelejpinto.productoapi.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@org.springframework.context.annotation.Profile("!test")
public class KafkaConfig {

    @Bean
    public NewTopic productoEventsTopic() {
        return TopicBuilder.name("producto-events")
            .partitions(3)
            .replicas(1)
            .build();
    }
}
