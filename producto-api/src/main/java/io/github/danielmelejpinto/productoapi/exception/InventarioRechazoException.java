package io.github.danielmelejpinto.productoapi.exception;

public class InventarioRechazoException extends RuntimeException {
    public InventarioRechazoException(String message, Throwable cause) {
        super(message, cause);
    }
    public InventarioRechazoException(String message) {
        super(message);
    }
}
