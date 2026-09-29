package io.github.danielmelejpinto.productoapi.exception;

public class InventarioNoDisponibleException extends RuntimeException {
    public InventarioNoDisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
