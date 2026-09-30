package com.edusistem.core.auth.domain.outputports;

import com.edusistem.core.auth.domain.vo.RateLimit;

/**
 * Limita cuántas veces se usa un endpoint sensible (registro, recuperación de contraseña) por clave (correo, IP...).
 * A diferencia de {@link LoginAttemptPort}, cuenta todas las peticiones, no solo las fallidas.
 */
public interface RequestRateLimitPort {

    /** Registra la petición si cabe en el límite y devuelve 0; si no cabe, no la registra y devuelve los segundos de espera. */
    long tryConsume(String key, RateLimit limit);
}
