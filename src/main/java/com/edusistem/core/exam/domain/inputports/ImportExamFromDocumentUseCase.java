package com.edusistem.core.exam.domain.inputports;

import com.edusistem.core.exam.application.use_case.dtos.ExamCommands;
import com.edusistem.core.exam.domain.vo.ExamDetails;
import java.util.List;

/** Crea exámenes o reemplaza sus preguntas a partir de un documento Word (.docx). */
public interface ImportExamFromDocumentUseCase {

    ExamDetails createFromDocument(ExamCommands.CreateFromDocument command);

    /** Igual que reemplazar las preguntas: no se permite si ya hay hojas procesadas. */
    ExamDetails replaceQuestionsFromDocument(ExamCommands.ReplaceQuestionsFromDocument command);

    /** Interpreta el documento sin guardar nada, para que el profesor revise las preguntas antes de importarlas. */
    List<ExamCommands.QuestionInput> preview(byte[] content);

    byte[] template();
}
