-- Fuera del DBML: permite revocar todos los JWT emitidos a un usuario (logout, cambio/reset de contraseña, reuso de refresh token).
ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0;
