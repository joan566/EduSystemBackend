package com.edusistem.core.imports.application.contracts;

import com.edusistem.core.imports.domain.entity.ImportBatch;
import com.edusistem.core.imports.domain.enums.ImportType;

/**
 * Recibe un Excel para importarlo en segundo plano: valida que sea un .xlsx, lo guarda, registra el batch en QUEUED y
 * lo encola. Lo que se pueda validar sin abrir el archivo se rechaza aquí, de forma síncrona.
 */
public interface ImportQueue {

    ImportBatch submit(ImportRequest request);

    /** @param teachingPeriodId solo para {@link ImportType#TEACHING_PERIOD} */
    record ImportRequest(Long teacherId, ImportType type, Long teachingPeriodId, String fileName,
                         String defaultFileName, byte[] content) {
    }
}
