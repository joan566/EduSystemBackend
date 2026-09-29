package com.edusistem.core.gradebook.domain.outputports;

import com.edusistem.core.gradebook.domain.entity.StudentObservation;
import java.util.Optional;

public interface StudentObservationRepositoryPort {

    Optional<StudentObservation> find(Long teachingPeriodId, Long studentId);

    StudentObservation save(StudentObservation observation);

    void delete(Long id);
}
