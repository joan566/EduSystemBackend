package com.edusistem.core.exam.domain.inputports;

import com.edusistem.core.exam.application.use_case.dtos.SubmissionCommands;
import com.edusistem.core.exam.domain.vo.SubmissionDetails;

public interface SubmitAnswerSheetUseCase {

    /** Procesa la imagen de una hoja: QR → estudiante → burbujas → puntuación → nota en la escala. */
    SubmissionDetails submit(SubmissionCommands.Submit command);
}
