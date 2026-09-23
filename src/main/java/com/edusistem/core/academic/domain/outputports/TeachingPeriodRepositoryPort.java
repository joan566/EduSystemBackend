package com.edusistem.core.academic.domain.outputports;

import com.edusistem.core.academic.domain.entity.TeachingPeriod;
import com.edusistem.core.academic.domain.vo.TeachingPeriodView;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.util.Optional;

public interface TeachingPeriodRepositoryPort {

    TeachingPeriod save(TeachingPeriod teachingPeriod);

    Optional<TeachingPeriod> findById(Long id);

    Optional<TeachingPeriodView> findViewById(Long id);

    Optional<TeachingPeriod> findByTeachingAssignmentIdAndAcademicPeriodId(Long teachingAssignmentId, Long academicPeriodId);

    PageResult<TeachingPeriodView> findViewsByTeacherId(Long teacherId, Long teachingAssignmentId, Long academicPeriodId,
                                                        PageQuery page);

    /** Tiene evaluaciones o configuración de calificación. */
    boolean hasDependents(Long teachingPeriodId);

    void deleteById(Long id);
}
