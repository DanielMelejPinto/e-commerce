package io.github.danielmelejpinto.productoapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient(
            @Value("${inventario.api.url}") String inventarioUrl,
            @Value("${inventario.api.connect-timeout-ms:2000}") int connectTimeout,
            @Value("${inventario.api.read-timeout-ms:5000}") int readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        
        return RestClient.builder()
                .baseUrl(inventarioUrl)
                .requestFactory(factory)
                .build();
    }
}
