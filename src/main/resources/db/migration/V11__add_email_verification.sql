-- Verificación del correo al registrarse. Las cuentas que ya existían quedan verificadas para no bloquearlas; las
-- nuevas las crea la app con email_verified = FALSE.
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ALTER COLUMN email_verified SET DEFAULT FALSE;

-- La misma tabla de códigos de un solo uso guarda los de recuperación de contraseña y los de verificación de correo;
-- purpose los separa para que pedir uno no invalide el otro (solo vale el último de cada tipo).
ALTER TABLE password_reset_tokens ADD COLUMN purpose VARCHAR(30) NOT NULL DEFAULT 'PASSWORD_RESET';
DROP INDEX idx_password_reset_tokens_user;
CREATE INDEX idx_password_reset_tokens_user ON password_reset_tokens (user_id, purpose, created_at DESC);
