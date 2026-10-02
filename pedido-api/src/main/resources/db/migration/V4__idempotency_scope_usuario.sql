CREATE TABLE claves_idempotencia_nueva (
    usuario_id BIGINT NOT NULL,
    clave VARCHAR(255) NOT NULL,
    pedido_id BIGINT NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL
);

INSERT INTO claves_idempotencia_nueva (usuario_id, clave, pedido_id, fecha_creacion)
SELECT p.usuario_id, c.clave, c.pedido_id, c.fecha_creacion
FROM claves_idempotencia c
JOIN pedidos p ON c.pedido_id = p.id;

DROP TABLE claves_idempotencia;

ALTER TABLE claves_idempotencia_nueva RENAME TO claves_idempotencia;
ALTER TABLE claves_idempotencia ADD PRIMARY KEY (usuario_id, clave);
ALTER TABLE claves_idempotencia ADD CONSTRAINT fk_idempotencia_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos(id);
