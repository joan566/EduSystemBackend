package com.edusistem.core.exam.domain.inputports;

import com.edusistem.core.exam.application.use_case.dtos.SubmissionCommands;
import com.edusistem.core.exam.domain.entity.SubmissionBatch;

public interface SubmitAnswerSheetBatchUseCase {

    /**
     * Valida el examen y el PDF, lo guarda y encola su procesamiento en segundo plano; devuelve el lote en QUEUED.
     * Cada página se califica después en su propia transacción.
     */
    SubmissionBatch start(SubmissionCommands.SubmitBatch command);
}
