package com.edusistem.core.shared.domain.outputports;

/**
 * Los catálogos académicos (grados, grupos, asignaturas, periodos) son compartidos entre profesores. Para que un
 * profesor no altere datos que otros usan, solo puede modificarlos si ningún OTRO profesor los tiene asignados.
 */
public interface CatalogUsagePort {

    boolean gradeUsedByOtherTeachers(Long gradeId, Long teacherId);

    boolean groupUsedByOtherTeachers(Long groupId, Long teacherId);

    boolean subjectUsedByOtherTeachers(Long subjectId, Long teacherId);

    boolean academicPeriodUsedByOtherTeachers(Long academicPeriodId, Long teacherId);
}
