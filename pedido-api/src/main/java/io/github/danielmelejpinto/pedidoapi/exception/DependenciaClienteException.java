package io.github.danielmelejpinto.pedidoapi.exception;

import org.springframework.http.HttpStatusCode;

public class DependenciaClienteException extends RuntimeException {
    private final HttpStatusCode statusCode;

    public DependenciaClienteException(String message, HttpStatusCode statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}
