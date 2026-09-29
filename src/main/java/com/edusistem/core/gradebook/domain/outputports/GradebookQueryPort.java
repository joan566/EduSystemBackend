package com.edusistem.core.gradebook.domain.outputports;

import com.edusistem.core.gradebook.domain.vo.EvaluationType;
import com.edusistem.core.gradebook.domain.vo.GradebookRow;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Lecturas que cruzan las especializaciones de evaluación (exámenes, actividades, asistencia). */
public interface GradebookQueryPort {

    /** Las evaluaciones de un teaching period con el resultado del estudiante, por fecha. */
    List<GradebookRow> findRows(Long teachingPeriodId, Long studentId);

    /** Especialización de una evaluación y, si es actividad, su id. */
    Optional<EvaluationKind> findKind(Long evaluationId);

    /** Evaluaciones del teaching period que tienen rúbrica. */
    Set<Long> evaluationIdsWithRubric(Long teachingPeriodId);

    record EvaluationKind(Long evaluationId, Long teachingPeriodId, EvaluationType type, BigDecimal maximumScore,
                          Long activityId) {
    }
}
