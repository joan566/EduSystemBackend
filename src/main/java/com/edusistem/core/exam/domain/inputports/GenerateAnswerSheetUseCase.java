package com.edusistem.core.exam.domain.inputports;

import com.edusistem.core.exam.domain.vo.PdfDocument;

public interface GenerateAnswerSheetUseCase {

    PdfDocument forStudent(Long teacherId, Long examId, Long studentId);

    /** Un PDF con una hoja por cada estudiante activo del grupo. */
    PdfDocument forGroup(Long teacherId, Long examId);
}
