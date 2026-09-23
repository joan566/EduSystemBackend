package com.edusistem.core.exam.infrastructure.repository;

import com.edusistem.core.exam.domain.enums.ExamSubmissionStatus;
import com.edusistem.core.exam.domain.vo.ExamSubmissionSummary;
import com.edusistem.core.exam.infrastructure.entity.ExamSubmissionEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataExamSubmissionRepository extends JpaRepository<ExamSubmissionEntity, Long> {

    Optional<ExamSubmissionEntity> findByExamIdAndStudentId(Long examId, Long studentId);

    boolean existsByExamId(Long examId);

    @Query(value = """
            select new com.edusistem.core.exam.domain.vo.ExamSubmissionSummary(s.id, s.studentId, s.studentCode,
                concat(st.lastName, ' ', st.firstName), s.status, s.score, s.finalGrade, s.statusDetail,
                s.submittedAt, s.processedAt)
            from ExamSubmissionEntity s join StudentEntity st on st.id = s.studentId
            where s.examId = :examId and (:status is null or s.status = :status)
            order by st.lastName, st.firstName, s.id""",
            countQuery = """
                    select count(s) from ExamSubmissionEntity s
                    where s.examId = :examId and (:status is null or s.status = :status)""")
    Page<ExamSubmissionSummary> findSummaries(@Param("examId") Long examId,
                                              @Param("status") ExamSubmissionStatus status, Pageable pageable);
}
