package com.edusistem.core.exam.domain.outputports;

import com.edusistem.core.exam.domain.vo.DocumentParagraph;
import java.util.List;

/** Lectura de documentos Word con preguntas y generación de su plantilla. */
public interface QuestionDocumentPort {

    /**
     * Párrafos del documento en orden de lectura (incluidos los de las tablas).
     * @throws com.edusistem.core.shared.domain.exceptions.InvalidRequestException si no es un .docx legible
     */
    List<DocumentParagraph> readParagraphs(byte[] content);

    /** Plantilla .docx con instrucciones y preguntas de ejemplo en el formato que entiende la importación. */
    byte[] template();
}
