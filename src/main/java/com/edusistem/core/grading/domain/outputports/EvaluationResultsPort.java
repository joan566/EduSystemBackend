package com.edusistem.core.grading.domain.outputports;

import com.edusistem.core.grading.domain.vo.EvaluationResult;
import java.util.List;

public interface EvaluationResultsPort {

    /** Resultados de cada estudiante activo del grupo en cada evaluación del teaching period (exámenes, actividades, asistencia). */
    List<EvaluationResult> findByTeachingPeriodId(Long teachingPeriodId);

    /** Existen exámenes calificados con final_grade en el periodo (bloquea el cambio de escala). */
    boolean hasGradedExamResults(Long teachingPeriodId);
}
