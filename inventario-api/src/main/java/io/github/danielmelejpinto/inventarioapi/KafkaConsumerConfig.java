package io.github.danielmelejpinto.inventarioapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

@Configuration
public class KafkaConsumerConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    /**
     * Reintenta los fallos del listener con backoff exponencial (1s, 2s, 4s, 8s, 16s).
     * Si se agotan los reintentos, o el mensaje no se puede deserializar, deja un ERROR
     * explicito con el registro afectado y sigue con el siguiente (no bloquea la particion).
     */
    @Bean
    CommonErrorHandler kafkaErrorHandler() {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(5);
        backOff.setInitialInterval(1_000L);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(16_000L);

        return new DefaultErrorHandler((record, ex) -> log.error(
                "Evento descartado tras agotar reintentos: topic={} partition={} offset={} key={} value={}",
                record.topic(), record.partition(), record.offset(), record.key(), record.value(), ex), backOff);
    }
}
