package com.edusistem.core.academic.domain.outputports;

import com.edusistem.core.academic.domain.entity.TeachingAssignment;
import com.edusistem.core.academic.domain.vo.TeachingAssignmentView;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.util.Optional;

public interface TeachingAssignmentRepositoryPort {

    TeachingAssignment save(TeachingAssignment assignment);

    Optional<TeachingAssignment> findById(Long id);

    Optional<TeachingAssignmentView> findViewById(Long id);

    Optional<TeachingAssignment> findByTeacherIdAndGroupIdAndSubjectId(Long teacherId, Long groupId, Long subjectId);

    PageResult<TeachingAssignmentView> findViewsByTeacherId(Long teacherId, Long groupId, Long subjectId, Boolean active,
                                                            PageQuery page);

    boolean hasTeachingPeriods(Long teachingAssignmentId);

    void deleteById(Long id);
}
