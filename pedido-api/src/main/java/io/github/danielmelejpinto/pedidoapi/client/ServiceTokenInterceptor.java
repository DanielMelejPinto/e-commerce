package io.github.danielmelejpinto.pedidoapi.client;

import io.github.danielmelejpinto.pedidoapi.security.JwtService;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Autentica las llamadas de pedido-api a inventario-api con un token de servicio (rol SYSTEM),
 * en lugar de reenviar el token del usuario final.
 */
@Component
public class ServiceTokenInterceptor implements ClientHttpRequestInterceptor {

    private final JwtService jwtService;

    public ServiceTokenInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().set("Authorization", "Bearer " + jwtService.generarTokenSistema());
        return execution.execute(request, body);
    }
}
