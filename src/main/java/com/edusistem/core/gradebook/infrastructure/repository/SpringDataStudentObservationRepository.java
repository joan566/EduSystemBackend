package com.edusistem.core.gradebook.infrastructure.repository;

import com.edusistem.core.gradebook.infrastructure.entity.StudentObservationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataStudentObservationRepository extends JpaRepository<StudentObservationEntity, Long> {

    Optional<StudentObservationEntity> findByTeachingPeriodIdAndStudentId(Long teachingPeriodId, Long studentId);
}
