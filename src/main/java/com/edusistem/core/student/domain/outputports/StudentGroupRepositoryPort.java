package com.edusistem.core.student.domain.outputports;

import com.edusistem.core.student.domain.entity.StudentGroup;
import com.edusistem.core.student.domain.vo.StudentEnrollmentView;
import java.util.List;
import java.util.Optional;

public interface StudentGroupRepositoryPort {

    StudentGroup save(StudentGroup studentGroup);

    Optional<StudentGroup> find(Long studentId, Long groupId);

    boolean existsActive(Long studentId, Long groupId);

    List<StudentEnrollmentView> findEnrollmentViews(Long studentId);
}
