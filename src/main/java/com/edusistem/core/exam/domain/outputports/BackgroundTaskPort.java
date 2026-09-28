package com.edusistem.core.exam.domain.outputports;

/** Ejecuta trabajo fuera del hilo de la petición HTTP. */
public interface BackgroundTaskPort {

    void run(Runnable task);
}
