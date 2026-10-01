package com.edusistem.core.exports.application.contracts;

import com.edusistem.core.exports.application.schoolsetup.SchoolSetupExportContext;

/** Carga una vez los datos que comparten varias hojas del export (grupos, clases, matrículas, escalas, actividades). */
public interface SchoolSetupSnapshotLoader {

    SchoolSetupExportContext load(Long teacherId);
}
