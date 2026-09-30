package com.edusistem.core.academic.infrastructure.repository;

import com.edusistem.core.academic.infrastructure.entity.AcademicPeriodEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAcademicPeriodRepository extends JpaRepository<AcademicPeriodEntity, Long> {

    Optional<AcademicPeriodEntity> findFirstByTeacherIdAndNameOrderByIdAsc(Long teacherId, String name);

    Page<AcademicPeriodEntity> findByTeacherId(Long teacherId, Pageable pageable);
}
