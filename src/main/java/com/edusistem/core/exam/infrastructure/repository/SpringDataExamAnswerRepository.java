package com.edusistem.core.exam.infrastructure.repository;

import com.edusistem.core.exam.infrastructure.entity.ExamAnswerEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataExamAnswerRepository extends JpaRepository<ExamAnswerEntity, Long> {

    List<ExamAnswerEntity> findBySubmissionIdOrderByIdAsc(Long submissionId);
}
