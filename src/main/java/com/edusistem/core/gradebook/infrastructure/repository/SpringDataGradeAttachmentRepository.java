package com.edusistem.core.gradebook.infrastructure.repository;

import com.edusistem.core.gradebook.infrastructure.entity.GradeAttachmentEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataGradeAttachmentRepository extends JpaRepository<GradeAttachmentEntity, Long> {

    Optional<GradeAttachmentEntity> findByEvaluationIdAndStudentId(Long evaluationId, Long studentId);

    @Query("""
            select a.evaluationId from GradeAttachmentEntity a, EvaluationEntity e
            where e.id = a.evaluationId and e.teachingPeriodId = :teachingPeriodId and a.studentId = :studentId""")
    List<Long> findEvaluationIds(@Param("teachingPeriodId") Long teachingPeriodId, @Param("studentId") Long studentId);
}
