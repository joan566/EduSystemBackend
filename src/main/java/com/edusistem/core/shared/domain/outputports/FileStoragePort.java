package com.edusistem.core.shared.domain.outputports;

import java.io.IOException;

public interface FileStoragePort {

    /** Guarda el contenido bajo el directorio lógico indicado y devuelve la ruta relativa almacenada. */
    String store(String directory, String fileName, byte[] content);

    byte[] read(String relativePath) throws IOException;

    boolean exists(String relativePath);
}
