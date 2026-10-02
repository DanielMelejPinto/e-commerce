package io.github.danielmelejpinto.pedidoapi.model;

import java.io.Serializable;
import java.util.Objects;

public class ClaveIdempotenciaId implements Serializable {
    private Long usuarioId;
    private String clave;

    public ClaveIdempotenciaId() {}

    public ClaveIdempotenciaId(Long usuarioId, String clave) {
        this.usuarioId = usuarioId;
        this.clave = clave;
    }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClaveIdempotenciaId that = (ClaveIdempotenciaId) o;
        return Objects.equals(usuarioId, that.usuarioId) &&
               Objects.equals(clave, that.clave);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usuarioId, clave);
    }
}
