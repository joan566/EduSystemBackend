package com.edusistem.core.auth.domain.outputports;

import java.util.List;

/** Borrado definitivo de una cuenta de profesor y de todos sus datos. */
public interface AccountErasurePort {

    /** Borra las filas; devuelve las rutas de archivos (hojas, adjuntos, PDF, Excel) que hay que borrar. */
    List<String> erase(Long userId, String email);
}
