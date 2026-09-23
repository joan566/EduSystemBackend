package com.edusistem.core.auth.domain.outputports;

/** Limita los intentos fallidos de login por correo y por IP para frenar la fuerza bruta. */
public interface LoginAttemptPort {

    /** Segundos que faltan para poder reintentar; 0 si no está bloqueado. */
    long retryAfterSeconds(String email, String clientIp);

    void recordFailure(String email, String clientIp);

    /** Un login correcto reinicia el contador del correo. */
    void recordSuccess(String email);
}
