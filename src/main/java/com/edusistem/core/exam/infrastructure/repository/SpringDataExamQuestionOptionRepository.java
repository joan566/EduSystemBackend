package com.edusistem.core.exam.infrastructure.repository;

import com.edusistem.core.exam.infrastructure.entity.ExamQuestionOptionEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataExamQuestionOptionRepository extends JpaRepository<ExamQuestionOptionEntity, Long> {

    List<ExamQuestionOptionEntity> findByQuestionIdInOrderByQuestionIdAscOptionLetterAsc(Collection<Long> questionIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ExamQuestionOptionEntity o where o.questionId in "
            + "(select q.id from ExamQuestionEntity q where q.examId = :examId)")
    void deleteByExamId(@Param("examId") Long examId);
}
