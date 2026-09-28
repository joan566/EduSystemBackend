package com.edusistem.core.exam.domain.inputports;

import com.edusistem.core.exam.domain.vo.PdfDocument;

public interface GenerateAnswerSheetUseCase {

    /** Hoja del estudiante; con {@code includeQuestions} le sigue el cuadernillo de preguntas. */
    PdfDocument forStudent(Long teacherId, Long examId, Long studentId, boolean includeQuestions);

    /** Un PDF con una hoja por cada estudiante activo del grupo (cada una seguida del cuadernillo si se pide). */
    PdfDocument forGroup(Long teacherId, Long examId, boolean includeQuestions);

    /** Solo el cuadernillo de preguntas (una copia, para fotocopiar). */
    PdfDocument questionBooklet(Long teacherId, Long examId);
}
