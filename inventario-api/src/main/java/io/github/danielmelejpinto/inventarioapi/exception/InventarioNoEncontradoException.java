package io.github.danielmelejpinto.inventarioapi.exception;

public class InventarioNoEncontradoException extends RuntimeException {

    public InventarioNoEncontradoException(Long productoId) {
        super("No existe inventario para el producto con id " + productoId);
    }
}