package com.edusistem.core.exports.application.contracts;

import com.edusistem.core.exports.application.schoolsetup.SchoolSetupExportContext;
import com.edusistem.core.shared.domain.vo.TabularData;

/**
 * Genera una hoja del libro "Configuración escolar" con los datos del profesor. Hay una implementación por hoja; el
 * caso de uso las escribe en el orden de {@link com.edusistem.core.shared.domain.vo.SchoolSetupSheets#sheetNames()}.
 */
public interface SchoolSetupSheetExporter {

    /** Nombre canónico de la hoja (una de las constantes de {@code SchoolSetupSheets}). */
    String sheetName();

    TabularData export(SchoolSetupExportContext context);
}
