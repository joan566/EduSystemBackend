package com.edusistem.core.subject.domain.outputports;

import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import com.edusistem.core.subject.domain.entity.Subject;
import java.util.Optional;

public interface SubjectRepositoryPort {

    Subject save(Subject subject);

    Optional<Subject> findById(Long id);

    Optional<Subject> findByTeacherIdAndName(Long teacherId, String name);

    PageResult<Subject> search(Long teacherId, String nameQuery, PageQuery page);

    boolean hasTeachingAssignments(Long subjectId);

    void deleteById(Long id);
}
