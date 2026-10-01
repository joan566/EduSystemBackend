package com.edusistem.core.imports.application.contracts;

import com.edusistem.core.imports.application.schoolsetup.ClassKey;
import java.util.Optional;

/** Resuelve una clase (teaching period) del profesor a partir de sus partes. */
public interface ClassResolver {

    /** Teaching period ya existente identificado por nombres; vacío si no existe la clase o alguna de sus partes. */
    Optional<Long> find(Long teacherId, ClassKey key);

    /** Teaching period del grupo, asignatura y periodo; crea la asignación y el teaching period si no existen. */
    Long findOrCreate(Long teacherId, Long groupId, Long subjectId, Long academicPeriodId);
}
