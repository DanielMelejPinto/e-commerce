CREATE TABLE claves_idempotencia (
    clave VARCHAR(255) PRIMARY KEY,
    pedido_id BIGINT NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    CONSTRAINT fk_idempotencia_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id)
);
