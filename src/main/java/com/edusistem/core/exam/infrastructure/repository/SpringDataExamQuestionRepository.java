package com.edusistem.core.exam.infrastructure.repository;

import com.edusistem.core.exam.infrastructure.entity.ExamQuestionEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataExamQuestionRepository extends JpaRepository<ExamQuestionEntity, Long> {

    List<ExamQuestionEntity> findByExamIdOrderByQuestionNumberAsc(Long examId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ExamQuestionEntity q where q.examId = :examId")
    void deleteByExamId(@Param("examId") Long examId);
}
