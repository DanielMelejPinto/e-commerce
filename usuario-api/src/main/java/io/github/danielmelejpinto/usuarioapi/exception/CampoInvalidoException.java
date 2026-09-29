package io.github.danielmelejpinto.usuarioapi.exception;

// Un campo inválido detectado en el service (lo que las anotaciones no alcanzan a validar)
public class CampoInvalidoException extends RuntimeException {

    private final String campo;

    public CampoInvalidoException(String campo, String mensaje) {
        super(mensaje);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}