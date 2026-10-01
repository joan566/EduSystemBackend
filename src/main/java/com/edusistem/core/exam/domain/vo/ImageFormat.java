package com.edusistem.core.exam.domain.vo;

import java.util.Optional;

/** Formatos de imagen aceptados para las hojas de respuesta, reconocidos por su firma (bytes iniciales). */
public enum ImageFormat {

    PNG("image/png"),
    JPEG("image/jpeg");

    private final String mediaType;

    ImageFormat(String mediaType) {
        this.mediaType = mediaType;
    }

    public String mediaType() {
        return mediaType;
    }

    /** Vacío si el contenido es nulo, está vacío o no es un PNG ni un JPEG. */
    public static Optional<ImageFormat> detect(byte[] content) {
        if (content != null && content.length > 8 && (content[0] & 0xFF) == 0x89 && content[1] == 'P'
                && content[2] == 'N') {
            return Optional.of(PNG);
        }
        if (content != null && content.length > 3 && (content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8) {
            return Optional.of(JPEG);
        }
        return Optional.empty();
    }
}
