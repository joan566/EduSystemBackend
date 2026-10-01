package com.edusistem.core.imports.application.contracts;

import com.edusistem.core.imports.application.schoolsetup.SchoolSetupImportContext;

/**
 * Importa una hoja del libro "Configuración escolar". Hay una implementación por hoja; el caso de uso las ejecuta en el
 * orden de {@link com.edusistem.core.shared.domain.vo.SchoolSetupSheets#sheetNames()} para que cada hoja encuentre lo
 * que crearon las anteriores.
 */
public interface SchoolSetupSheetImporter {

    /** Nombre canónico de la hoja (una de las constantes de {@code SchoolSetupSheets}). */
    String sheetName();

    /**
     * Valida y aplica las filas de la hoja. Los errores por fila se agregan al contexto; una columna obligatoria que
     * falta lanza una excepción que aborta la importación.
     * @return cantidad de filas de la hoja (0 si el libro no la trae)
     */
    int importSheet(SchoolSetupImportContext context);
}
