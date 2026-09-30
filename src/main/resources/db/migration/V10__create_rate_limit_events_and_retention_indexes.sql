-- Eventos de los límites de peticiones (login fallido, registro, recuperación de contraseña). Viven en la base para
-- que el límite sea común a todas las instancias y sobreviva a reinicios; se purgan a los pocos días.
CREATE TABLE rate_limit_events (
    id          BIGSERIAL    PRIMARY KEY,
    bucket      VARCHAR(320) NOT NULL,
    occurred_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_rate_limit_events_bucket ON rate_limit_events (bucket, occurred_at);
CREATE INDEX idx_rate_limit_events_occurred ON rate_limit_events (occurred_at);

-- Purga diaria por antigüedad de la auditoría y de los códigos de recuperación
CREATE INDEX idx_audit_logs_created ON audit_logs (created_at);
CREATE INDEX idx_password_reset_tokens_expires ON password_reset_tokens (expires_at);
