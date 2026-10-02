ALTER TABLE claves_idempotencia ADD COLUMN hash_contenido VARCHAR(255);
ALTER TABLE claves_idempotencia ADD COLUMN estado VARCHAR(50);

-- Actualizar registros existentes
UPDATE claves_idempotencia SET estado = 'COMPLETADO', hash_contenido = 'MIGRADO' WHERE estado IS NULL;

ALTER TABLE claves_idempotencia ALTER COLUMN hash_contenido SET NOT NULL;
ALTER TABLE claves_idempotencia ALTER COLUMN estado SET NOT NULL;
