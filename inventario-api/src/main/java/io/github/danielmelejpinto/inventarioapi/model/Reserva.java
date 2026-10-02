package io.github.danielmelejpinto.inventarioapi.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "reserva", uniqueConstraints = {
    @UniqueConstraint(name = "reserva_pedido_producto_uk", columnNames = {"pedido_id", "producto_id"})
})
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reserva_id")
    private Long id;

    @Column(name = "pedido_id", nullable = false)
    private Long pedidoId;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "cantidad", nullable = false)
    private Long cantidad;

    @Column(name = "fecha_reserva", nullable = false, columnDefinition = "TIMESTAMP(6)")
    private LocalDateTime fechaReserva;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado = "ACTIVA";

    @PrePersist
    protected void onCreate() {
        this.fechaReserva = LocalDateTime.now();
    }

    public Reserva() {}

    public Reserva(Long pedidoId, Long productoId, Integer cantidad) {
        this.pedidoId = pedidoId;
        this.productoId = productoId;
        this.cantidad = cantidad.longValue();
        this.estado = "ACTIVA";
    }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPedidoId() { return pedidoId; }
    public void setPedidoId(Long pedidoId) { this.pedidoId = pedidoId; }

    public Long getProductoId() { return productoId; }
    public void setProductoId(Long productoId) { this.productoId = productoId; }

    public Long getCantidad() { return cantidad; }
    public void setCantidad(Long cantidad) { this.cantidad = cantidad; }

    public LocalDateTime getFechaReserva() { return fechaReserva; }
    public void setFechaReserva(LocalDateTime fechaReserva) { this.fechaReserva = fechaReserva; }
}
