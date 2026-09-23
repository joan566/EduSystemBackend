package com.edusistem.core.imports.domain.inputports;

import com.edusistem.core.imports.application.use_case.dtos.ImportCommands;
import com.edusistem.core.imports.domain.vo.ImportResult;

public interface ImportStudentsUseCase {

    ImportResult importStudents(ImportCommands.ImportStudents command);

    /** Plantilla .xlsx vacía con los encabezados esperados y una fila de ejemplo. */
    byte[] template();
}
