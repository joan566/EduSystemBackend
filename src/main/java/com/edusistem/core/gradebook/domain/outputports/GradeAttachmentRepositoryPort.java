package com.edusistem.core.gradebook.domain.outputports;

import com.edusistem.core.gradebook.domain.entity.GradeAttachment;
import java.util.Optional;
import java.util.Set;

public interface GradeAttachmentRepositoryPort {

    Optional<GradeAttachment> find(Long evaluationId, Long studentId);

    GradeAttachment save(GradeAttachment attachment);

    void delete(Long id);

    /** Evaluaciones del teaching period en las que el estudiante tiene adjunto. */
    Set<Long> evaluationIdsWithAttachment(Long teachingPeriodId, Long studentId);
}
