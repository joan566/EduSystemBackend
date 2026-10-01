package com.edusistem.core.imports.domain.inputports;

import com.edusistem.core.imports.application.use_case.dtos.ImportCommands;
import com.edusistem.core.imports.domain.entity.ImportBatch;

public interface ImportStudentsUseCase {

    /** Encola el Excel: devuelve el batch en QUEUED y se importa en segundo plano. */
    ImportBatch importStudents(ImportCommands.ImportStudents command);

    /** Plantilla .xlsx vacía con los encabezados esperados y una fila de ejemplo. */
    byte[] template();
}
