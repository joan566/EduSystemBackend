package com.edusistem.core.student.domain.inputports;

/** Derecho de supresión: elimina al estudiante con todas sus notas, asistencias, hojas de respuesta y adjuntos. */
public interface EraseStudentUseCase {

    void erase(Long teacherId, Long studentId);
}
