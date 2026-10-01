package com.edusistem.core.imports.infrastructure.adapter;

import com.edusistem.core.imports.domain.inputports.ProcessImportBatchUseCase;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Al arrancar, reencola las importaciones que un reinicio dejó en QUEUED/PROCESSING. */
@Component
public class ImportBatchRecovery {

    private final ProcessImportBatchUseCase imports;

    public ImportBatchRecovery(ProcessImportBatchUseCase imports) {
        this.imports = imports;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resume() {
        imports.resumeUnfinished();
    }
}
