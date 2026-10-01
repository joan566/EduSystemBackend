package com.edusistem.core.imports.application.contracts;

import java.util.Map;

/** Estudiantes activos de un grupo, indexados por número de identificación (los que no tienen uno se omiten). */
public interface StudentRoster {

    Map<String, Long> activeByIdentificationInGroup(Long groupId);

    /** Igual que {@link #activeByIdentificationInGroup}, para el grupo de un teaching period. */
    Map<String, Long> activeByIdentificationInTeachingPeriod(Long teachingPeriodId);
}
