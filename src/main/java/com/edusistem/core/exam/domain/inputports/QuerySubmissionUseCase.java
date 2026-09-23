package com.edusistem.core.exam.domain.inputports;

import com.edusistem.core.exam.domain.enums.ExamSubmissionStatus;
import com.edusistem.core.exam.domain.vo.ExamSubmissionSummary;
import com.edusistem.core.exam.domain.vo.ImageFile;
import com.edusistem.core.exam.domain.vo.SubmissionDetails;
import com.edusistem.core.shared.domain.vo.PageQuery;
import com.edusistem.core.shared.domain.vo.PageResult;

public interface QuerySubmissionUseCase {

    SubmissionDetails get(Long teacherId, Long examId, Long submissionId);

    /** Foto original de la hoja tal como se subió. */
    ImageFile image(Long teacherId, Long examId, Long submissionId);

    PageResult<ExamSubmissionSummary> search(Long teacherId, Long examId, ExamSubmissionStatus status, PageQuery page);
}
