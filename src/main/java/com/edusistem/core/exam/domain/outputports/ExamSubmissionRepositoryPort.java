package com.edusistem.core.exam.domain.outputports;

import com.edusistem.core.exam.domain.entity.ExamSubmission;
import com.edusistem.core.exam.domain.enums.ExamSubmissionStatus;
import com.edusistem.core.exam.domain.vo.ExamSubmissionSummary;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;
import java.util.Optional;

public interface ExamSubmissionRepositoryPort {

    /** Guarda la submission y sincroniza sus respuestas (actualiza las existentes, inserta nuevas, borra las ausentes). */
    ExamSubmission save(ExamSubmission submission);

    /** Con respuestas cargadas. */
    Optional<ExamSubmission> findById(Long id);

    Optional<ExamSubmission> findByExamIdAndStudentId(Long examId, Long studentId);

    PageResult<ExamSubmissionSummary> findSummariesByExamId(Long examId, ExamSubmissionStatus status, PageQuery page);
}
