package io.github.danielmelejpinto.pedidoapi.exception;

public class EstadoPedidoInvalidoException extends RuntimeException {
    public EstadoPedidoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
