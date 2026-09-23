package com.edusistem.core.academic.domain.outputports;

import com.edusistem.core.academic.domain.entity.Grade;
import java.util.List;
import java.util.Optional;

public interface GradeRepositoryPort {

    Grade save(Grade grade);

    Optional<Grade> findById(Long id);

    Optional<Grade> findByName(String name);

    List<Grade> findAllOrderedByName();

    boolean hasGroups(Long gradeId);

    void deleteById(Long id);
}
