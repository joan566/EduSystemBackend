package com.edusistem.core.exports.domain.inputports;

import com.edusistem.core.exports.domain.vo.ExportedFile;

public interface ExportSchoolSetupUseCase {

    /**
     * Toda la configuración del profesor en el mismo libro que lee {@code POST /imports/school-setup}: periodos,
     * grados, asignaturas, grupos, clases (con escala, nota mínima y pesos), horarios, estudiantes, actividades, notas y
     * asistencia. Se puede volver a subir sin crear duplicados.
     */
    ExportedFile schoolSetup(Long teacherId);
}
