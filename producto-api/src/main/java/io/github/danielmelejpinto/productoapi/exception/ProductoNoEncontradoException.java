package io.github.danielmelejpinto.productoapi.exception;

public class ProductoNoEncontradoException extends RuntimeException {

    public ProductoNoEncontradoException(Long id) {
        super("Producto con id " + id + " no existe");
    }
}