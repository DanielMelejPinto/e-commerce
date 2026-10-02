package io.github.danielmelejpinto.pedidoapi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "claves_idempotencia")
@IdClass(ClaveIdempotenciaId.class)
public class ClaveIdempotencia {

    @Id
    private Long usuarioId;

    @Id
    private String clave;

    @Column(nullable = false)
    private Long pedidoId;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }

    public ClaveIdempotencia() {}

    public ClaveIdempotencia(Long usuarioId, String clave, Long pedidoId) {
        this.usuarioId = usuarioId;
        this.clave = clave;
        this.pedidoId = pedidoId;
    }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
