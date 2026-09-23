package com.edusistem.core.imports.domain.inputports;

import com.edusistem.core.imports.application.use_case.dtos.ImportCommands;
import com.edusistem.core.imports.domain.vo.ImportResult;

public interface ImportSchoolSetupUseCase {

    ImportResult importData(ImportCommands.ImportSchoolSetup command);

    byte[] template();
}
