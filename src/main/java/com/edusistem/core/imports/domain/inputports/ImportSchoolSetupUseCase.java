package com.edusistem.core.imports.domain.inputports;

import com.edusistem.core.imports.application.use_case.dtos.ImportCommands;
import com.edusistem.core.imports.domain.entity.ImportBatch;

public interface ImportSchoolSetupUseCase {

    /** Encola el Excel: devuelve el batch en QUEUED y se importa en segundo plano. */
    ImportBatch importData(ImportCommands.ImportSchoolSetup command);

    byte[] template();
}
