package io.github.danielmelejpinto.pedidoapi.exception;

public class ServicioDependienteException extends RuntimeException {
    public ServicioDependienteException(String message) {
        super(message);
    }
    public ServicioDependienteException(String message, Throwable cause) {
        super(message, cause);
    }
}
