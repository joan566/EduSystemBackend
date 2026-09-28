package com.edusistem.core.exam.domain.outputports;

/** Convierte un PDF escaneado en imágenes de página que el procesador de hojas puede leer. */
public interface ScannedPdfPort {

    /** Recibe cada página (número base 1) como imagen JPEG/PNG, o nula si esa página no pudo rasterizarse. */
    @FunctionalInterface
    interface PageHandler {
        void handle(int pageNumber, byte[] image);
    }

    /**
     * Número de páginas, validando el archivo.
     * @throws com.edusistem.core.shared.domain.exceptions.InvalidRequestException si no es un PDF legible, no tiene
     *         páginas o supera {@code maxPages}
     */
    int pageCount(byte[] pdf, int maxPages);

    /** Rasteriza una a una (sin cargarlas todas en memoria) las páginas desde {@code firstPage} (base 1), en orden. */
    void forEachPage(byte[] pdf, int firstPage, PageHandler handler);
}
