package com.edusistem.core.exam.infrastructure.adapter;

import com.edusistem.core.exam.domain.inputports.ProcessSubmissionBatchUseCase;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Al arrancar, reencola los lotes que un reinicio dejó en QUEUED/PROCESSING. */
@Component
public class SubmissionBatchRecovery {

    private final ProcessSubmissionBatchUseCase batches;

    public SubmissionBatchRecovery(ProcessSubmissionBatchUseCase batches) {
        this.batches = batches;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resume() {
        batches.resumeUnfinished();
    }
}
