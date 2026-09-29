package io.github.danielmelejpinto.inventarioapi.exception;

public class StockInsuficienteException extends RuntimeException {

    public StockInsuficienteException(Long productoId, long disponible, int solicitado) {
        super("Stock insuficiente para el producto " + productoId
                + ": disponible " + disponible + ", solicitado " + solicitado);
    }
}