package io.github.danielmelejpinto.pedidoapi.exception;

public class PedidoNoEncontradoException extends RuntimeException {
    public PedidoNoEncontradoException(Long id) {
        super("Pedido no encontrado o no pertenece al usuario: " + id);
    }
}
