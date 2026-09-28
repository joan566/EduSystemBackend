package com.edusistem.core.exam.application.use_case.service;

import com.edusistem.core.exam.application.use_case.dtos.SubmissionCommands;
import com.edusistem.core.exam.domain.entity.SubmissionBatchPage;
import com.edusistem.core.exam.domain.enums.BatchPageOutcome;
import com.edusistem.core.exam.domain.inputports.SubmitAnswerSheetUseCase;
import com.edusistem.core.exam.domain.outputports.SubmissionBatchRepositoryPort;
import com.edusistem.core.exam.domain.vo.SubmissionDetails;
import com.edusistem.core.shared.application.transaction.UseCaseTransactional;

/**
 * Califica la hoja de una página y registra la página en el lote en UNA transacción: tras un reinicio no puede quedar
 * una nota guardada sin su página (que al reanudar se reprocesaría y chocaría con SUBMISSION_ALREADY_EXISTS).
 * Si la hoja se rechaza, la excepción revierte todo y el worker registra la página rechazada aparte.
 */
public class SubmissionBatchPageRecorder {

    private final SubmitAnswerSheetUseCase single;
    private final SubmissionBatchRepositoryPort batches;

    public SubmissionBatchPageRecorder(SubmitAnswerSheetUseCase single, SubmissionBatchRepositoryPort batches) {
        this.single = single;
        this.batches = batches;
    }

    @UseCaseTransactional
    public SubmissionBatchPage submitAndRecord(SubmissionCommands.Submit command, SubmissionBatchPage page) {
        SubmissionDetails details = single.submit(command);
        page.setOutcome(switch (details.submission().getStatus()) {
            case PROCESSED -> BatchPageOutcome.PROCESSED;
            case REVIEW_REQUIRED -> BatchPageOutcome.REVIEW_REQUIRED;
            default -> BatchPageOutcome.FAILED;
        });
        page.setMessage(details.submission().getStatusDetail());
        page.setSubmissionId(details.submission().getId());
        return batches.savePage(page);
    }
}
