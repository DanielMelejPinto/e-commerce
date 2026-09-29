package io.github.danielmelejpinto.usuarioapi.exception;

public class EmailYaRegistradoException extends RuntimeException {

    public EmailYaRegistradoException() {
        super("El email ya está registrado");
    }
}