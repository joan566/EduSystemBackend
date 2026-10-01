package com.edusistem.core.imports.domain.outputports;

/** Ejecuta una importación fuera del hilo de la petición HTTP. */
public interface ImportBackgroundTaskPort {

    void run(Runnable task);
}
