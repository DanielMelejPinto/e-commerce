package io.github.danielmelejpinto.pedidoapi.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "claves_idempotencia")
public class ClaveIdempotencia {

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

    public ClaveIdempotencia(String clave, Long pedidoId) {
        this.clave = clave;
        this.pedidoId = pedidoId;
    }

    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
