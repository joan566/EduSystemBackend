package com.edusistem.core.gradebook.domain.outputports;

import com.edusistem.core.gradebook.domain.entity.RubricCriterion;
import com.edusistem.core.gradebook.domain.entity.RubricScore;
import java.util.Collection;
import java.util.List;

public interface RubricRepositoryPort {

    /** Criterios de la evaluación en orden. */
    List<RubricCriterion> findCriteria(Long evaluationId);

    List<RubricCriterion> saveCriteria(List<RubricCriterion> criteria);

    /** Borra los criterios (y en cascada sus puntajes). */
    void deleteCriteria(Collection<Long> criterionIds);

    List<RubricScore> findScores(Long evaluationId, Long studentId);

    void saveScores(List<RubricScore> scores);
}
