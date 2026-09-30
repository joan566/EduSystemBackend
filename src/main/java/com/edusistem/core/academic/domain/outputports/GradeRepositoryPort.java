package com.edusistem.core.academic.domain.outputports;

import com.edusistem.core.academic.domain.entity.Grade;
import java.util.List;
import java.util.Optional;

public interface GradeRepositoryPort {

    Grade save(Grade grade);

    Optional<Grade> findById(Long id);

    Optional<Grade> findByTeacherIdAndName(Long teacherId, String name);

    List<Grade> findByTeacherIdOrderedByName(Long teacherId);

    boolean hasGroups(Long gradeId);

    void deleteById(Long id);
}
