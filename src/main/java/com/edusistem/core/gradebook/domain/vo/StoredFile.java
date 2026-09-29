package com.edusistem.core.gradebook.domain.vo;

/** Contenido de un archivo para descargar. */
public record StoredFile(String fileName, String contentType, byte[] content) {
}
