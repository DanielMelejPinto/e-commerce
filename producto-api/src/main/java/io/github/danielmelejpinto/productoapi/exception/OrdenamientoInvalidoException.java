package io.github.danielmelejpinto.productoapi.exception;

import java.util.List;

public class OrdenamientoInvalidoException extends RuntimeException {

    public OrdenamientoInvalidoException(String campo, List<String> permitidos) {
        super("No se puede ordenar por '" + campo + "'. Campos permitidos: " + String.join(", ", permitidos));
    }
}